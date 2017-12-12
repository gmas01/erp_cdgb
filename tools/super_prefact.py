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


def setup_logger(debug):
    """setup logger singleton"""

    # if no name is specified, return a logger
    # which is the root logger of the hierarchy.
    root = logging.getLogger()

    # create console handler with a higher log level
    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG if debug else logging.INFO)

    # add the handlers to root
    root.addHandler(ch)

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


def incept_prefact(profile_path, debug, user_id, cust_id):
    """creates global prefactura"""

    logger = setup_logger(debug)
    pt = read_settings(logger, profile_path)
    conn = open_dbms_conn(logger, pt.dbms.pgsql_conn)

    try:
        validation(conn, user_id)
        prefact_id = create(conn, user_id, cust_id)
        clean(conn, prefact_id)
    except:
        raise
    finally:
        if conn is not None:
            conn.close()


def validation(conn, user_id):
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
    return 0


def clean(conn, prefact_id):
    pass


if __name__ == "__main__":

    args = parse_cmdline()

    RESOURCES_DIR = '{}/resources'.format(expanduser("~"))
    PROFILES_DIR = '{}/profiles'.format(RESOURCES_DIR)
    DEFAULT_PROFILE = 'default.json'

    profile_path = '{}/{}'.format(PROFILES_DIR,
            args.config if args.config else DEFAULT_PROFILE)
    try:
        incept_prefact(profile_path, args.debug, args.user_id, args.cust_id)
    except:
        if args.debug:
            print('Whoops! a problem came up!')
            traceback.print_exc(file=sys.stderr)
        sys.exit(1)

    # assuming everything went right, exit gracefully
    sys.exit(0)
