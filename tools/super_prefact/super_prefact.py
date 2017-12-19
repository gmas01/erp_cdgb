#!/usr/bin/python3

import time
import os
import traceback
import argparse
import sys
import json
import logging
import subprocess
from distutils.spawn import find_executable
from os.path import expanduser

sys.path.append(
    os.path.abspath(
        os.path.join(expanduser("~"), "cfdiengine")
    )
)

from custom.profile import ProfileReader
from misc.helperpg import HelperPg
from misc.tricks import dump_exception

class CfdiEngineTrigger(object):

    __JAVA_BIN = "java"

    def __init__(self, host, port, err_mute = False):
        self.__err_mute = err_mute
        self.__host = host
        self.__port = port

        def seekout_java():
            executable = find_executable(self.__JAVA_BIN)
            if executable:
                return os.path.abspath(executable)
            raise Exception("it has not found {} binary".format(self.__JAVA_BIN))

        self.__java_bin = seekout_java()
        self.__java_args = ['-classpath', './jars/*',
                'com.agnux.tcp.App', self.__host, str(self.__port)]

        self.__cmd_tokens = [self.__java_bin] + self.__java_args

    def __call__(self, in_b, cmd_timeout):
        """implementation as function object"""

        def time_gap(delta):
            t = time.time()
            return t, t + delta

        def monitor(p, tbegin, tend):
            """Loop until process returns or timeout expires"""
            rc = None
            output = ''
            while time.time() < tend and rc is None:
                rc = p.poll()
                if rc is None:
                    try:
                        outs, errs = p.communicate(input=in_b, timeout=100)
                        output += outs.decode("utf-8")
                    except subprocess.TimeoutExpired:
                        pass
            return output, rc

        if self.__err_mute:
            out_err = subprocess.DEVNULL
        else:
            out_err = subprocess.STDOUT

        oput, rc = monitor(
            subprocess.Popen(
                self.__cmd_tokens,
                stdin = subprocess.PIPE,
                stdout = subprocess.PIPE,
                stderr = out_err
            ),
            *time_gap(cmd_timeout)
        )

        if rc is None:
            raise subprocess.TimeoutExpired(
                cmd = self.__cmd_tokens,
                output = oput,
                timeout = cmd_timeout
            )

        if rc == 0:
            return oput

        raise subprocess.CalledProcessError(
            returncode = rc,
            cmd = self.__cmd_tokens,
            output = oput
        )


def parse_cmdline():
    """parses the command line arguments at the call."""

    psr_desc = "super prefactura tool"
    psr_epi = "select a config profile to specify defaults"

    psr = argparse.ArgumentParser(
            description=psr_desc, epilog=psr_epi)

    psr.add_argument('-d', action='store_true', dest='debug',
            help='print debug information')

    psr.add_argument('-c', '--config', action='store',
            dest='config', help='load an specific config profile')

    psr.add_argument('-uid', '--user_id', action='store',
            dest='user_id', help='specify the user id')

    psr.add_argument('-cid', '--cust_id', action='store',
            dest='cust_id', help='specify the customer id')

    psr.add_argument('-ho', '--host', action='store',
            dest='host', help='hostname of cfdiengine microservice')

    psr.add_argument('-po', '--port', action='store',
            dest='port', help='port of cfdiengine microservice')

    return psr.parse_args()


def setup_logger(debug):
    """setup logger singleton"""

    # if no name is specified, return a logger
    # which is the root logger of the hierarchy.
    root = logging.getLogger(__name__)

    logging.basicConfig(level=logging.DEBUG if debug else logging.INFO)

    return root


def read_settings(logger, s_file):
    """reads profile"""

    logger.debug("looking for config profile file in:\n{0}".format(
        os.path.abspath(s_file)))
    if os.path.isfile(s_file):
        reader = ProfileReader(logger)
        return reader(s_file)
    raise Exception("unable to locate the config profile file")


def open_dbms_conn(logger, pgsql_conf):
    """opens a connection to postgresql"""

    try:
        return HelperPg.connect(pgsql_conf)
    except psycopg2.Error as e:
        logger.error(e)
        raise Exception("dbms was not connected")
    except KeyError as e:
        logger.error(e)
        raise Exception("slack pgsql configuration")


def incept_prefact(logger, pt, debug, user_id, cust_id, rme):
    """creates global prefactura"""

    conn = open_dbms_conn(logger, pt.dbms.pgsql_conn)

    try:
        validation(conn, user_id)
        prefact_id = create(conn, user_id, cust_id)
        out = facturar(conn, user_id, prefact_id, rme)
        logger.debug(out)
    except:
        raise
    finally:
        if conn is not None:
            conn.close()


def validation(conn, user_id):
    """checks coherency before creation prefactura"""

    q = """SELECT *
        FROM fac_global_validation( {}::integer )
        AS result( rc integer, msg text )""".format(user_id)

    res = HelperPg.query(conn, q, True)
    if len(res) != 1:
        raise Exception('unexpected result regarding execution of fac_global_validation')

    rcode, rmsg = res.pop()
    if rcode != 0:
        raise Exception(rmsg)


def create(conn, user_id, cust_id):
    """creates a prefactura"""

    q = '''SELECT *
        FROM fac_global_prefact( {}::integer, {}::integer )'''.format(user_id, cust_id)

    res = HelperPg.query(conn, q, True)
    if len(res) != 1:
        raise Exception('unexpected result regarding execution of fac_global_prefact')

    return (res.pop())[0]


def facturar(conn, user_id, prefact_id, rme):

    def _q_no_id_empresa():
        q = """SELECT gral_emp.no_id::character varying as no_id
            FROM gral_usr_suc
            JOIN gral_suc ON gral_suc.id = gral_usr_suc.gral_suc_id
            JOIN gral_emp ON gral_emp.id = gral_suc.empresa_id
            WHERE gral_usr_suc.gral_usr_id ="""
        for row in HelperPg.query(conn, "{0}{1}".format(q, user_id)):
            # Just taking first row of query result
            return row['no_id']

    def _q_serie_folio():
        q = """select fac_cfds_conf_folios.serie as serie,
            fac_cfds_conf_folios.folio_actual::character varying as folio
            FROM gral_suc AS SUC
            LEFT JOIN fac_cfds_conf ON fac_cfds_conf.gral_suc_id = SUC.id
            LEFT JOIN fac_cfds_conf_folios ON fac_cfds_conf_folios.fac_cfds_conf_id = fac_cfds_conf.id
            LEFT JOIN gral_usr_suc AS USR_SUC ON USR_SUC.gral_suc_id = SUC.id
            WHERE fac_cfds_conf_folios.proposito = 'FAC'
            AND USR_SUC.gral_usr_id="""
        for row in HelperPg.query(conn, "{0}{1}".format(q, user_id)):
            # Just taking first row of query result
            return {
                'SERIE': row['serie'],
                'FOLIO': row['folio']
            }

    filename = None
    try:
        no_id = _q_no_id_empresa()
        sf = _q_serie_folio()
        filename = '{}_{}{}.xml'.format(no_id, sf['SERIE'], sf['FOLIO'])
    except:
        raise Exception("Error conforming filename variable for request")

    request = json.dumps(
        {
            'request': {
                'to': 'cxc',
                'action': 'facturar',
                'args': {
                    'filename': filename,
                    'prefact_id': prefact_id,
                    'usr_id': user_id
                }
            }
        }
    ).encode('utf-8')

    return rme(request, cmd_timeout = 100)


if __name__ == "__main__":

    args = parse_cmdline()

    RESOURCES_DIR = '{}/resources'.format(expanduser("~"))
    PROFILES_DIR = '{}/profiles'.format(RESOURCES_DIR)
    DEFAULT_PROFILE = 'default.json'

    profile_path = '{}/{}'.format(PROFILES_DIR,
            args.config if args.config else DEFAULT_PROFILE)

    logger = setup_logger(args.debug)
    logger.debug(args)

    rme = CfdiEngineTrigger(args.host, args.port)

    try:
        pt = read_settings(logger, profile_path)
        incept_prefact(logger, pt, args.debug, args.user_id, args.cust_id, rme)
        logger.debug('successful super prefact execution')
    except subprocess.CalledProcessError as e:
        print(e.output)
    except:
        if args.debug:
            print('Whoops! a problem came up!')
            print(dump_exception())
        sys.exit(1)

    # assuming everything went right, exit gracefully
    sys.exit(0)
