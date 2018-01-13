import tempfile
import os
from engine.error import ErrorCode
from misc.helperpg import HelperPg
from engine.common import fetch_rdirs
from pac.connector import setup_pac
from misc.tricks import dump_exception
from custom.profile import ProfileReader
from sat.reader import SaxReader


def undofacturar(logger, pt, req):

    fact_id = req.get('fact_id', None)
    usr_id = req.get('usr_id', None)
    reason = req.get('reason', None)
    mode = req.get('mode', None)

    if reason is None:
        reason = ''

    if (fact_id is None) or (usr_id is None) or (mode is None):
        return ErrorCode.REQUEST_INCOMPLETE.value

    q_val = """select * from fac_val_cancel( {}::integer )
        AS result( rc integer, msg text )
        """.format(   # Store procedure parameters
            fact_id   #  _fac_id
    )

    q_do = """select * from fac_exec_cancel(
        {}::integer,
        {}::integer,
        {}::text,
        {}::integer
        ) AS result( rc integer, msg text )
        """.format(   # Store procedure parameters
        usr_id,       #  _usr_id
        fact_id,      #  _fac_id
        reason,       #  _reason
        mode          #  _mode
    )

    def run_store(q):
        logger.debug("Performing query: {}".format(q))
        res = HelperPg.onfly_query(pt.dbms.pgsql_conn, q, True)

        # For this case we are just expecting one row
        if len(res) != 1:
            raise Exception('unexpected result regarding execution of store')
        return res


    def check_result(r):
        rcode, rmsg = r.pop()
        if rcode != 0:
            raise Exception(rmsg)


    def get_xml_name():
        q = """select ref_id as filename
            FROM fac_docs
            WHERE fac_docs.id="""

        for row in HelperPg.onfly_query(pt.dbms.pgsql_conn, "{0}{1}".format(q, fact_id), True):
            # Just taking first row of query result
            return row['filename'] + '.xml'


    def get_emisor_rfc():
        q = """select upper(EMP.rfc) as rfc
            FROM gral_suc AS SUC
            LEFT JOIN gral_usr_suc AS USR_SUC ON USR_SUC.gral_suc_id = SUC.id
            LEFT JOIN gral_emp AS EMP ON EMP.id = SUC.empresa_id
            LEFT JOIN cfdi_regimenes AS REG ON REG.numero_control = EMP.regimen_fiscal
            WHERE USR_SUC.gral_usr_id="""

        for row in HelperPg.onfly_query(pt.dbms.pgsql_conn, "{0}{1}".format(q, usr_id), True):
            # Just taking first row of query result
            return row['rfc']


    def pac_cancel(t, rfc):
        try:
            logger.debug('Getting a pac connector as per config profile')
            pac, err = setup_pac(logger, pt.tparty.pac)
            if pac is None:
                raise Exception(err)

            s_cancel = pac.cancel(t, rfc)
            logger.debug(s_cancel)

            return ErrorCode.SUCCESS
        except:
            logger.error(dump_exception())
            return ErrorCode.THIRD_PARTY_ISSUES

    source = ProfileReader.get_content(pt.source, ProfileReader.PNODE_UNIQUE)
    resdir = os.path.abspath(os.path.join(os.path.dirname(source), os.pardir))
    rdirs = fetch_rdirs(resdir, pt.res.dirs)

    _uuid = None
    _res = None
    _rfc = get_emisor_rfc()

    try:
        cfdi_dir = os.path.join(rdirs['cfdi_output'], _rfc)
        f_xml = os.path.join(cfdi_dir, get_xml_name())
        logger.debug('File to cancel {}'.format(f_xml))
        parser = SaxReader()
        xml_dat, _ = parser(f_xml)
        _uuid = xml_dat['UUID']
    except:
        return ErrorCode.RESOURCE_NOT_FOUND.value

    try:
        _res = run_store(q_val)
    except:
        logger.error(dump_exception())
        return ErrorCode.DBMS_SQL_ISSUES.value

    try:
        check_result(_res)
    except:
        logger.error(dump_exception())
        return ErrorCode.REQUEST_INVALID.value

    rc = pac_cancel(_uuid, _rfc)
    if rc != ErrorCode.SUCCESS:
        return rc.value

    try:
        _res = run_store(q_do)
    except:
        logger.error(dump_exception())
        return ErrorCode.DBMS_SQL_ISSUES.value

    try:
        check_result(_res)
    except:
        logger.error(dump_exception())
        return ErrorCode.DBMS_TRANS_ERROR.value

    return ErrorCode.SUCCESS.value


def facturar(logger, pt, req):

    from docmaker.pipeline import DocPipeLine
    from misc.helperstr import HelperStr

    def inception_pdf(f_outdoc, resdir, f_xmlin, inceptor_rfc):
        dm_builder = 'facpdf'
        kwargs = {'xml': f_xmlin, 'rfc': inceptor_rfc}
        try:
            dpl = DocPipeLine(logger, resdir,
                rdirs_conf=pt.res.dirs,
                pgsql_conf=pt.dbms.pgsql_conn)
            dpl.run(dm_builder, f_outdoc, **kwargs)
            return ErrorCode.SUCCESS
        except:
            logger.error(dump_exception())
            return ErrorCode.DOCMAKER_ERROR

    def inception_xml(f_outdoc, resdir, usr_id, prefact_id):
        dm_builder = 'facxml'
        kwargs = {'usr_id': usr_id, 'prefact_id': prefact_id}
        try:
            dpl = DocPipeLine(logger, resdir,
                rdirs_conf=pt.res.dirs,
                pgsql_conf=pt.dbms.pgsql_conn)
            dpl.run(dm_builder, f_outdoc, **kwargs)
            return ErrorCode.SUCCESS
        except:
            logger.error(dump_exception())
            return ErrorCode.DOCMAKER_ERROR

    def fetch_empdat(usr_id):
        sql = """select upper(EMP.rfc) as rfc, EMP.no_id as no_id
            FROM gral_suc AS SUC
            LEFT JOIN gral_usr_suc AS USR_SUC ON USR_SUC.gral_suc_id = SUC.id
            LEFT JOIN gral_emp AS EMP ON EMP.id = SUC.empresa_id
            WHERE USR_SUC.gral_usr_id="""
        q = "{0}{1}".format(sql, usr_id)
        logger.debug("Performing query: {}".format(q))
        try:
            for row in HelperPg.onfly_query(pt.dbms.pgsql_conn, q):
                return ErrorCode.SUCCESS, dict(rfc=row['rfc'], no_id=row['no_id'])
        except:
            logger.error(dump_exception())
            return ErrorCode.DBMS_SQL_ISSUES, None

    def pac_sign(f_xmlin, xid, out_dir):
        try:
            # Here it would be placed, code calling
            # the pac connector mechanism

            logger.debug('Getting a pac connector as per config profile')
            pac, err = setup_pac(logger, pt.tparty.pac)
            if pac is None:
                raise Exception(err)

            logger.debug('File to sign {}'.format(f_xmlin))

            s_signed = None
            with open(f_xmlin) as f:
                s_signed = pac.stamp(f.read(), xid)
                logger.debug(s_signed)

            f_xmlout = os.path.join(out_dir, xid)
            logger.debug('saving pac xml signed upon {}'.format(f_xmlout))
            with open(f_xmlout, "w") as f:
                f.write(s_signed)

            return ErrorCode.SUCCESS, f_xmlout
        except:
            logger.error(dump_exception())
            return ErrorCode.THIRD_PARTY_ISSUES, None

    def store(f_xmlin, usr_id, prefact_id, no_id):
        parser = SaxReader()
        xml_dat, _ = parser(f_xmlin)
        ref_id = '{}_{}{}'.format(no_id, xml_dat['CFDI_SERIE'], xml_dat['CFDI_FOLIO'])
        q = """select fac_save_xml from fac_save_xml(
            '{}'::character varying, {}::integer, {}::integer, '{}'::character varying,
            '{}'::character varying, '{}'::character varying, '{}'::character varying,
            '{}'::character varying, '{}'::character varying, '{}'::character varying,
            '{}'::character varying, '{}'::character varying, '{}'::character varying,
            '{}'::character varying, '{}'::character varying, '{}'::character varying,
            '{}'::character varying, '{}'::character varying, '{}'::character varying,
             {}::double precision, {}::double precision, {}::double precision, {}::boolean
        )""".format(                             # Store procedure parameters
            os.path.basename(f_xmlin),           # file_xml
            prefact_id,                          # prefact_id
            usr_id,                              # usr_id
            xml_dat['CFDI_DATE'].split('T')[0],  # creation_date
            ref_id,                              # no_id_emp
            xml_dat['CFDI_SERIE'],               # serie
            xml_dat['CFDI_FOLIO'],               # folio
            'THIS FIELD IS DEPRECATED',          # items_str
            'THIS FIELD IS DEPRECATED',          # traslados_str
            'THIS FIELD IS DEPRECATED',          # retenciones_str
            xml_dat['INCEPTOR_REG'],             # reg_fiscal
            'THIS FIELD IS DEPRECATED',          # pay_method
            xml_dat['INCEPTOR_CP'],              # exp_place
            'FACTURA',                           # proposito      - It is obviously hardcoded
            'THIS FIELD IS DEPRECATED',          # no_aprob
            'THIS FIELD IS DEPRECATED',          # ano_aprob
            xml_dat['RECEPTOR_RFC'],             # rfc_custm      - RFC customer
            xml_dat['RECEPTOR_NAME'],            # rs_custm       - Razon social customer
            '0000',                              # account_number - An account fake number invented by me
            xml_dat['TAXES']['TRAS']['TOTAL'],   # total_tras
            '0',                                 # subtotal_with_desc
            xml_dat['CFDI_TOTAL'],               # total
            'false'                              # refact
        )
        logger.debug("Performing query: {}".format(q))
        try:
            s_out = None
            for row in HelperPg.onfly_query(pt.dbms.pgsql_conn, q, True):
                # Just taking first row of query result
                s_out = row['fac_save_xml']
                break

            # here we should parse s_out line
            logger.debug('store procedure fac_save_xml has returned: {}'.format(s_out))

            return ErrorCode.SUCCESS
        except:
            logger.error(dump_exception())
            return ErrorCode.ETL_ISSUES

    logger.info("stepping in factura handler within {}".format(__name__))

    filename = req.get('filename', None)
    usr_id = req.get('usr_id', None)
    prefact_id = req.get('prefact_id', None)

    source = ProfileReader.get_content(pt.source, ProfileReader.PNODE_UNIQUE)
    resdir = os.path.abspath(os.path.join(os.path.dirname(source), os.pardir))
    rdirs = fetch_rdirs(resdir, pt.res.dirs)

    logger.debug('Using as resources directory {}'.format(resdir))

    tmp_dir = tempfile.gettempdir()
    tmp_file = os.path.join(tmp_dir, HelperStr.random_str())
    rc = inception_xml(tmp_file, resdir, usr_id, prefact_id)

    if rc == ErrorCode.SUCCESS:
        rc, inceptor_data = fetch_empdat(usr_id)
        if rc == ErrorCode.SUCCESS:
            out_dir = os.path.join(rdirs['cfdi_output'], inceptor_data['rfc'])
            rc, outfile = pac_sign(tmp_file, filename, out_dir)
            if rc == ErrorCode.SUCCESS:
                rc = store(outfile, usr_id, prefact_id, inceptor_data['no_id'])
            if rc == ErrorCode.SUCCESS:
                rc = inception_pdf(
                    outfile.replace('.xml', '.pdf'),    # We replace the xml extension
                    resdir, outfile, inceptor_data['rfc']
                )

    if os.path.isfile(tmp_file):
        os.remove(tmp_file)

    return rc.value
