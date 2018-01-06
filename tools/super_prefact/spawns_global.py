#!/usr/bin/python3

import os
import traceback
import argparse
import sys
import json
import logging

from os.path import expanduser

sys.path.append(
    os.path.abspath(
        os.path.join(expanduser("~"), "cfdiengine")
    )
)

from custom.profile import ProfileReader
from misc.helperpg import HelperPg
from misc.tricks import dump_exception
from engine.erp import do_request
from engine.error import ErrorCode



def incept_prefact(logger, pt, user_id, cust_id):
    """creates global prefactura"""

    conn = open_dbms_conn(logger, pt.dbms.pgsql_conn)

    prefact_id = None

    try:
        validation(conn, user_id)
        prefact_id = create(conn, user_id, cust_id)
        facturar(conn, logger, pt, user_id, prefact_id)
        make_remain_prefacts_up(conn, user_id)
    except:
        if prefact_id is not None:
            remove_bad_atempt(conn, prefact_id)
        raise
    finally:
        if conn is not None:
            conn.close()


def facturar(conn, logger, pt, user_id, prefact_id):

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
    )

    rc = do_request(logger, pt, request.encode('utf-8'))

    # Success error code must be always zero
    __SUCCESS = 0
    msg = "cfdiengine reply code: {}".format(rc)
    if rc == __SUCCESS:
        logger.debug(msg)
        return

    # We should have not reached this point
    raise Exception(msg)


def remove_bad_atempt(conn, prefact_id):
    logger.debug("Executing sql to remove attempt failed".format(prefact_id))
    q = """SELECT *
        FROM fac_global_rm_attempt( {}::integer )
        AS result( rc integer, msg text )""".format(prefact_id)

    res = HelperPg.query(conn, q, True)
    if len(res) != 1:
        raise Exception('unexpected result regarding execution of fac_global_bad_atempt')

    rcode, rmsg = res.pop()
    if rcode != 0:
        raise Exception(rmsg)


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


def make_remain_prefacts_up(conn, user_id):
    """deals with the remain prefacts which are opened"""

    q_upt = """UPDATE erp_proceso
        SET proceso_flujo_id = 3, id_aux = 2
        FROM gral_suc, gral_usr, gral_usr_suc
        WHERE gral_suc.id = gral_usr_suc.gral_suc_id
        AND gral_usr_suc.gral_usr_id = gral_usr.id
        AND erp_proceso.sucursal_id = gral_usr_suc.gral_suc_id
        AND erp_proceso.proceso_flujo_id = 2
        AND gral_usr_suc.gral_usr_id = {}""".format(user_id)

    nrowu = HelperPg.update(conn, q_upt)
    return nrowu


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

    return psr.parse_args()


if __name__ == "__main__":

    args = parse_cmdline()

    RESOURCES_DIR = '{}/resources'.format(expanduser("~"))
    PROFILES_DIR = '{}/profiles'.format(RESOURCES_DIR)
    DEFAULT_PROFILE = 'default.json'

    profile_path = '{}/{}'.format(PROFILES_DIR,
            args.config if args.config else DEFAULT_PROFILE)

    logger = setup_logger(args.debug)
    logger.debug(args)

    try:
        pt = read_settings(logger, profile_path)
        incept_prefact(logger, pt, args.user_id, args.cust_id)
        logger.debug('successful super prefact execution')
    except:
        if args.debug:
            print('Whoops! a problem came up!')
            print(dump_exception())
        sys.exit(1)

    # assuming everything went right, exit gracefully
    sys.exit(0)
