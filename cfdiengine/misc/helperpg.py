from custom.profile import ProfileReader
import psycopg2
import psycopg2.extras


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
