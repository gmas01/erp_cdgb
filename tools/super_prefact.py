import traceback
import sys
import psycopg2
import psycopg2.extras
import json

class ProfileTree:
    """object to chain other config nodes"""

    def __init__(self, data):
        self.data=data

    def __getattr__(self, key):

        try:
            return ProfileTree(self.data[key])
        except TypeError:
            result=[]
            for item in self.data:
                if key in item:
                    try:
                        result.append(item[key])
                    except TypeError: pass
            return ProfileTree(result)

    def __getitem__(self, key):

        return ProfileTree(self.data[key])

    def __iter__(self):

        if isinstance(self.data, str):
            # self.data might be a str or unicode object
            yield self.data
        else:
            # self.data might be a list or tuple
            try:
                for item in self.data:
                    yield item
            except TypeError:
                # self.data might be an int or float
                yield self.data

    def __length_hint__(self):
        return len(self.data)


class ProfileReader(object):
    """
    create a profile tree as per
    a determined config profile
    """

    PNODE_UNIQUE, PNODE_MANY = range(2)

    def __init__(self, logger):
        self.__logger = logger


    def __call__(self, p_file_path):

        def parse_profile():
            """
            Parses a profile in json format
            """
            try:
                json_lines = open(p_file_path).read()
                parsed_json = json.loads(json_lines)
                return parsed_json['engine_profile']
            except (KeyError, OSError, IOError) as e:
                self.__logger.error(e)
                self.__logger.fatal(
                    "malformed profile file in: {0}".format(p_file_path)
                )
                raise

        try:
            prof = parse_profile()
            prof['source'] = p_file_path
            return ProfileTree(prof)
        except:
            raise

    @staticmethod
    def get_content(pt_node, flavor):
        return [
            lambda : list(pt_node)[0],
            lambda : list(pt_node)
        ][flavor]()


class HelperPg(object):
    """
    """

    @staticmethod
    def connect(conf):
        """opens a connection to database"""
        try:
            conn_str = "dbname={0} user={1} host={2} password={3} port={4}".format(
                ProfileReader.get_content(conf.db, ProfileReader.PNODE_UNIQUE),
                ProfileReader.get_content(conf.user, ProfileReader.PNODE_UNIQUE),
                ProfileReader.get_content(conf.host, ProfileReader.PNODE_UNIQUE),
                ProfileReader.get_content(conf.passwd, ProfileReader.PNODE_UNIQUE),
                ProfileReader.get_content(conf.port, ProfileReader.PNODE_UNIQUE)
            )
            return psycopg2.connect(conn_str)
        except:
            raise Exception('It is not possible to connect with database')

    @staticmethod
    def query(conn, sql, commit=False):
        """carries an sql query out to database"""
        cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
        cur.execute(sql)
        if commit:
            conn.commit()
        rows = cur.fetchall()
        cur.close()
        if len(rows) > 0:
            return rows

        # We should not have reached this point
        raise Exception('There is not data retrieved')

    @staticmethod
    def onfly_query(conf, sql, commit=False):
        """exec a query with a temporary connection"""
        conn = HelperPg.connect(conf)

        try:
            return HelperPg.query(conn, sql, commit)
        except:
            raise
        finally:
            conn.close()

    @staticmethod
    def store(conn, name, output_expected, *args):
        """calls an store procedure of database"""
        cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
        cur.callproc(name, *args)
        rows = cur.fetchall()
        cur.close()

        if not output_expected:
            return

        if len(rows) > 0:
            return rows

        # We should not have reached this point
        raise Exception('There is not data retrieved')


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

    return psr.parse_args()


def incept_prefact(profile_path):
    pass


if __name__ == "__main__":

    args = parse_cmdline()

    RESOURCES_DIR = '{}/resources'.format(expanduser("~"))
    PROFILES_DIR = '{}/profiles'.format(RESOURCES_DIR)
    DEFAULT_PROFILE = 'default.json'

    profile_path = '{}/{}'.format(PROFILES_DIR,
            args.config if args.config else DEFAULT_PROFILE)
    try:
        incept_prefact(profile_path)
    except:
        if args.debug:
            print('Whoops! a problem came up!')
            traceback.print_exc(file=sys.stderr)
        sys.exit(1)

    # assuming everything went right, exit gracefully
    sys.exit(0)
