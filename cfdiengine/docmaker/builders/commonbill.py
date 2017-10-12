from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Table, TableStyle, Paragraph, Spacer, Image
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.units import cm 
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.pdfgen import canvas
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT, TA_JUSTIFY

class CommonBill(BuilderGen):

    __captions = {
        'SPA': {
            'TL_DOC_LANG': 'ESPAÑOL',
            'TL_DOC_NAME': 'FACTURA',
            'TL_DOC_DATE': 'FECHA Y HORA',
            'TL_DOC_OBS': 'OBSERVACIONES',
            'TL_CUST_NAME': 'CLIENTE',
            'TL_CUST_ADDR': 'DIRECCIÓN',
            'TL_CUST_ZIPC': 'C.P.',
            'TL_CUST_REG': 'R.F.C',
            'TL_CUST_NUM': 'NO. DE CLIENTE',
            'TL_ORDER_NUM': 'NO. DE ORDEN',
            'TL_BILL_CURR': 'MONEDA',
            'TL_BILL_EXC_RATE': 'TIPO DE CAMBIO',
            'TL_PAY_DATE': 'FECHA DE PAGO',
            'TL_SALE_MAN': 'AGENTE DE VENTAS',
            'TL_PAY_COND': 'CONDICIONES DE PAGO',
            'TL_ACC_NUM': 'NO. DE CUENTA',
            'TL_PAY_MET': 'METODO DE PAGO',
            'TL_PAY_WAY': 'FORMA DE PAGO',
            'TL_ART_SKU': 'CLAVE',
            'TL_ART_DES': 'DESCRIPCIÓN',
            'TL_ART_UNIT': 'UNIDAD',
            'TL_ART_QUAN': 'CANTIDAD',
            'TL_ART_UP': 'P. UNITARIO',
            'TL_ART_AMNT': 'IMPORTE',
            'TL_ART_SUBT': 'SUB-TOTAL',
            'TL_ART_TOTAL': 'TOTAL'
        },
        'ENG': {
            'TL_DOC_LANG': 'ENGLISH',
            'TL_DOC_NAME': 'INVOICE',
            'TL_DOC_DATE': 'DATE',
            'TL_DOC_OBS': 'OBS',
            'TL_CUST_NAME': 'CUSTOMER',
            'TL_CUST_ADDR': 'ADDRESS SOLD TO',
            'TL_CUST_ZIPC': 'ZIP CODE',
            'TL_CUST_REG': 'TAX ID',
            'TL_CUST_NUM': 'CUSTOMER #',
            'TL_ORDER_NUM': 'ORDER #',
            'TL_BILL_CURR': 'CURRENCY',
            'TL_BILL_EXC_RATE': 'EXCHANGE RATE',
            'TL_PAY_DATE': 'PAYMENT DATE',
            'TL_SALE_MAN': 'SALE REP',
            'TL_PAY_COND': 'PAYMENT TERMS',
            'TL_ACC_NUM': 'ACCOUNT #',
            'TL_PAY_MET': 'PAYMENT METHOD',
            'TL_PAY_WAY': 'TERMS',
            'TL_ART_SKU': 'SKU',
            'TL_ART_DES': 'DESCRIPTION',
            'TL_ART_UNIT': 'MEASURE',
            'TL_ART_QUAN': 'QUANTITY',
            'TL_ART_UP': 'UNIT PRICE',
            'TL_ART_AMNT': 'AMOUNT',
            'TL_ART_SUBT': 'SUBT',
            'TL_ART_TOTAL': 'TOTAL'
        }

    def __init__(self, logger):
        super().__init__(logger)

    def __info_stamp_table(self, t0, t1):
        cont = [[t0, t1]]
        table = Table(cont,
            [
                4.0 * cm,
                16.0 * cm
            ],
            [
                4.0*cm
            ]
        )
        table.setStyle( TableStyle([
            ('BOX', (0, 0), (-1, -1), 0.25, colors.black),
            ('VALIGN', (0, 0),(-1, -1), 'MIDDLE'),
            ('ALIGN', (0, 0),(0, 0), 'CENTER'),
            ('ALIGN', (1, 0),(1, 0), 'LEFT'),
            ('BACKGROUND', (1, 0),(1, 0), colors.sandybrown),
            ('LINEBEFORE',(1,0),(1,0), 0.25, colors.black)
        ]))
        return table

    def __create_seals_table(self, dat):
        cont = []
        st = ParagraphStyle(name='seal',fontName='Helvetica', fontSize=6.5, leading = 8)
        cont.append([ "CADENA ORIGINAL DEL TIMBRE:" ])
        cont.append([ Paragraph( dat['STAMP_ORIGINAL_STR'], st ) ])
        cont.append([ "SELLO DIGITAL DEL EMISOR:" ])
        cont.append([ Paragraph( dat['CFD_SEAL'], st ) ])
        cont.append([ "SELLO DIGITAL DEL SAT:" ])
        cont.append([ Paragraph( dat['SAT_SEAL'], st ) ])

        t = Table(
            cont,
            [
                15.5 * cm
            ],
            [
                0.4*cm,
                0.9*cm,
                0.4*cm,
                0.6*cm,
                0.4*cm,
                0.6*cm
            ]
        )
        t.setStyle( TableStyle([
            ('FONT', (0, 0), (0, 0), 'Helvetica-Bold', 6.5),
            ('FONT', (0, 2), (0, 2), 'Helvetica-Bold', 6.5),
            ('FONT', (0, 4), (0, 4), 'Helvetica-Bold', 6.5),
        ]))
        return t

    def __create_extra_sec(self, dat):

        cont = []
        cont.append([ dat['CAP_LOADED']['TL_CUST_NUM'], dat['CAP_LOADED']['TL_PAY_MET'] ])
        cont.append([ dat['CUSTOMER_CONTROL_ID'], dat['METODO_PAGO'] ])
        cont.append([ dat['CAP_LOADED']['TL_ORDER_NUM'], dat['CAP_LOADED']['TL_PAY_COND'] ])
        cont.append([ dat['PURCHASE_NUMBER'], dat['PAYMENT_CONSTRAINT'] ])
        cont.append([ dat['CAP_LOADED']['TL_BILL_CURR'] , dat['CAP_LOADED']['TL_PAY_WAY']])
        cont.append([ dat['CURRENCY_ABR'], dat['FORMA_PAGO'] ])
        cont.append([ dat['CAP_LOADED']['TL_BILL_EXC_RATE'], dat['CAP_LOADED']['TL_ACC_NUM'] ])
        cont.append([ dat['XML_PARSED']['MONEY_EXCHANGE'], dat['NO_CUENTA'] ])
        cont.append([ dat['CAP_LOADED']['TL_PAY_DATE'], dat['CAP_LOADED']['TL_SALE_MAN'] ])
        cont.append([ dat['PAYMENT_DATE'], dat['SALES_MAN'] ])

        table = Table(cont,
            [
                4.0 * cm,
                7.0 * cm   # rowWitdhs
            ],
            [0.35*cm] * 10 # rowHeights
        )
        table.setStyle( TableStyle([
            #Body and header look and feel (common)
            ('VALIGN', (0,0),(-1,-1), 'MIDDLE'),
            ('BOX', (0, 0), (-1, -1), 0.25, colors.black),
            ('TEXTCOLOR', (0,0),(-1,-1), colors.black),
            ('FONT', (0, 0), (-1, 0), 'Helvetica-Bold', 7),
            ('FONT', (0, 1), (-1, 1), 'Helvetica', 7),
            ('FONT', (0, 2), (-1, 2), 'Helvetica-Bold', 7),
            ('FONT', (0, 3), (-1, 3), 'Helvetica', 7),
            ('FONT', (0, 4), (-1, 4), 'Helvetica-Bold', 7),
            ('FONT', (0, 5), (-1, 5), 'Helvetica', 7),
            ('FONT', (0, 6), (-1, 6), 'Helvetica-Bold', 7),
            ('FONT', (0, 7), (-1, 7), 'Helvetica', 7),
            ('FONT', (0, 8), (-1, 8), 'Helvetica-Bold', 7),
            ('FONT', (0, 9), (-1, 9), 'Helvetica', 7),
            ('ROWBACKGROUNDS', (0, 0),(-1, -1), [colors.sandybrown, colors.white]),
            ('ALIGN', (0, 1),(-1, -1), 'LEFT'),
        ]))
        return table

    def __create_customer_sec(self, dat):
        cont = []
        cont.append([ dat['CAP_LOADED']['TL_CUST_NAME'] ])
        cont.append([ dat['RECEPTOR_NAME'].upper() ])
        cont.append([ dat['CAP_LOADED']['TL_CUST_REG'] ] )
        cont.append([ dat['RECEPTOR_RFC'].upper() ])
        cont.append([ dat['CAP_LOADED']['TL_CUST_ADDR'] ])
        cont.append([ (
            "{0} {1}".format(
                dat['RECEPTOR_STREET'],
                dat['RECEPTOR_STREET_NUMBER']
            )
        ).upper() ])
        cont.append([ dat['RECEPTOR_SETTLEMENT'].upper() ])
        cont.append([ "{0}, {1}".format(
            dat['RECEPTOR_TOWN'],
            dat['RECEPTOR_STATE']
        ).upper()])
        cont.append([ dat['RECEPTOR_COUNTRY'].upper() ])
        cont.append([ "%s %s" % ( dat['CAP_LOADED']['TL_CUST_ZIPC'], dat['RECEPTOR_CP']) ])

        table = Table(cont,
            [
                8.6 * cm   # rowWitdhs
            ],
            [0.35*cm] * 10 # rowHeights
        )
        table.setStyle( TableStyle([
            #Body and header look and feel (common)
            ('ROWBACKGROUNDS', (0, 0),(-1, 4), [colors.sandybrown, colors.white]),
            ('ALIGN', (0, 1),(-1, -1), 'LEFT'),
            ('VALIGN', (0,0),(-1,-1), 'MIDDLE'),
            ('BOX', (0, 0), (-1, -1), 0.25, colors.black),
            ('TEXTCOLOR', (0,0),(-1,-1), colors.black),
            ('FONT', (0, 0), (-1, 0), 'Helvetica-Bold', 7),
            ('FONT', (0, 1), (-1, 1), 'Helvetica', 7),
            ('FONT', (0, 2), (-1, 2), 'Helvetica-Bold', 7),
            ('FONT', (0, 3), (-1, 3), 'Helvetica', 7),
            ('FONT', (0, 4), (-1, 4), 'Helvetica-Bold', 7),
            ('FONT', (0, 5), (-1, 9), 'Helvetica', 7),
        ]))
        return table

    def __amount_table(self, t0, t1):
        cont = [[t0,t1]]
        table = Table(cont,
            [
                12.4 * cm,
                8 * cm
            ],
            [1.31 * cm] * len(cont) # rowHeights
        )
        table.setStyle(TableStyle([
            ('ALIGN', (0, 0),(0, 0), 'LEFT'),
            ('ALIGN', (-1, -1),(-1, -1), 'RIGHT'),
        ]))
        return table

    def __legend_table(self, dat):
        if len(dat['BILL_LEGENDS']) == 0:
            return None

        st = ParagraphStyle(name='info', alignment=TA_CENTER, fontName='Helvetica', fontSize=7, leading = 7)
        cont = []

        for l in dat['BILL_LEGENDS']:
            row = [
                Paragraph(l, st)
            ]
            cont.append(row)

        table = Table(cont,
            [
                20.0 * cm
            ]
        )

    def __create_letra_section(self, dat):
        cont = [ [''], ["IMPORTE CON LETRA"] ]
        (c,d) = dat['CFDI_TOTAL'].split('.')
        n = numspatrans(c)
        result = "{0} {1} {2}/100 {3}".format(
            n.upper(),
            dat['CURRENCY_NAME'],
            d,
            dat['CURRENCY_ABR']
        )

        # substitute multiple whitespace with single whitespace
        cont.append([ ' '.join(result.split()) ] )

        table_letra = Table(cont,
            [
                12.3 * cm  # rowWitdhs
            ],
            [0.4*cm] * len(cont) # rowHeights
        )
        table_letra.setStyle(TableStyle([
            ('VALIGN', (0,0),(-1,-1), 'MIDDLE'),
            ('ALIGN',  (0,0),(-1,-1), 'LEFT'),
            ('FONT', (0, 1), (-1, 1), 'Helvetica-Bold', 7),
            ('FONT', (0, 2), (-1, 2), 'Helvetica', 7),
        ]))
        return table_letra

    def __create_total_section(self, dat):
        cont = [[
            dat['CAP_LOADED']['TL_ART_SUBT'],
            dat['CURRENCY_ABR'],
            currency_format(
               __chomp_extra_zeroes(dat['CFDI_SUBTOTAL'])
            )
        ]]

        for imptras in dat['TAXES']['TRAS']['DETAILS']:
            (tasa, _) = imptras['TASA'].split('.')

            row = [
                "{0} {1}%".format(
                    'TAX' if dat['CAP_LOADED']['TL_DOC_LANG'] == 'ENGLISH' else imptras['IMPUESTO'],
                    tasa
                ),
                dat['CURRENCY_ABR'],
                currency_format(__chomp_extra_zeroes(imptras['IMPORTE']))
            ]
            cont.append(row)

        cont.append([
            dat['CAP_LOADED']['TL_ART_TOTAL'], dat['CURRENCY_ABR'],
            currency_format(self.__chomp_extra_zeroes(dat['CFDI_TOTAL']))
        ])

        table_total = Table(cont,
            [
                3.8 * cm,
                1.28* cm,
                2.5 * cm  # rowWitdhs
            ],
            [0.4*cm] * len(cont) # rowHeights
        )

        table_total.setStyle( TableStyle([
            ('VALIGN', (0,0),(-1,-1), 'MIDDLE'),
            ('ALIGN',  (0,0),(-1,-1), 'RIGHT'),
            ('BOX', (0, 0), (-1, -1), 0.25, colors.black),

            ('FONT', (0, 0), (0, -1), 'Helvetica-Bold', 7),

            ('BOX', (1, 0), (2, -1), 0.25, colors.black),

            ('FONT', (1, 0), (1, 1), 'Helvetica', 7),
            ('FONT', (1, 2), (1, 2), 'Helvetica-Bold', 7),
            ('FONT', (-1, 0), (-1, -1), 'Helvetica-Bold', 7),
        ]))
        return table_total

    def __create_arts_section(self, dat):
        add_currency_simbol = lambda c: '${0:>40}'.format(c)
        st = ParagraphStyle(
            name='info',
            fontName='Helvetica',
            fontSize=7,
            leading = 8
        )
        header_concepts = (
            dat['CAP_LOADED']['TL_ART_SKU'], dat['CAP_LOADED']['TL_ART_DES'],
            dat['CAP_LOADED']['TL_ART_UNIT'], dat['CAP_LOADED']['TL_ART_QUAN'],
            dat['CAP_LOADED']['TL_ART_UP'], dat['CAP_LOADED']['TL_ART_AMNT']
        )
        cont_concepts = []
        for i in dat['ARTIFACTS']:
            row = [
                    i['NOIDENTIFICACION'],
                    Paragraph( i['DESCRIPCION'], st),
                    i['UNIDAD'].upper(),
                    currency_format(__chomp_extra_zeroes(i['CANTIDAD'])),
                    add_currency_simbol(currency_format(__chomp_extra_zeroes(i['VALORUNITARIO']))),
                    add_currency_simbol(currency_format(__chomp_extra_zeroes(i['IMPORTE'])))
            ]
            cont_concepts.append(row)

        cont = [header_concepts] + cont_concepts

        table = Table(cont,[
                2.2 * cm,
                5.6 * cm,
                2.3 * cm,
                2.3 * cm,
                3.8 * cm,
                3.8 * cm])

        table.setStyle(TableStyle([

            #Body and header look and feel (common)
            ('ALIGN', (0,0),(-1,0), 'CENTER'),
            ('VALIGN', (0,0),(-1,-1), 'TOP'),
            ('BOX', (0, 0), (-1, 0), 0.25, colors.black),
            ('BACKGROUND', (0,0),(-1,0), colors.black),
            ('TEXTCOLOR', (0,0),(-1,0), colors.white),
            ('FONT', (0, 0), (-1, -1), 'Helvetica', 7),
            ('FONT', (0, 0), (-1, 0), 'Helvetica-Bold', 7),
            ('ROWBACKGROUNDS', (0, 1),(-1, -1), [colors.white, colors.sandybrown]),
            ('ALIGN', (0, 1),(1, -1), 'LEFT'),
            ('ALIGN', (2, 0),(2, -1), 'CENTER'),
            ('ALIGN', (3, 1),(-1, -1), 'RIGHT'),

            #Clave column look and feel (specific)
            ('BOX', (0, 1), (0, -1), 0.25, colors.black),

            #Description column look and feel (specific)
            ('BOX', (1, 1), (1, -1), 0.25, colors.black),

            #Unit column look and feel (specific)
            ('BOX', (2, 1), (2, -1), 0.25, colors.black),

            #Amount column look and feel (specific)
            ('BOX', (3, 1),(3, -1), 0.25, colors.black),

            #Amount column look and feel (specific)
            ('BOX', (4, 1),(4, -1), 0.25, colors.black),

            #Amount column look and feel (specific)
            ('BOX', (5, 1),(5, -1), 0.25, colors.black),

            #Amount column look and feel (specific)
            ('BOX', (6, 1),(6, -1), 0.25, colors.black),

            #Amount column look and feel (specific)
            ('BOX', (7, 1),(7, -1), 0.25, colors.black),
        ]))
        return table

    def __customer_table(self, t0, t1):
        cont = [[t0,t1]]
        table = Table(cont,
            [
                8.4 * cm,
                12 * cm
            ]
        )
        table.setStyle( TableStyle([
            ('ALIGN', (0, 0),(0, 0), 'LEFT'),
            ('ALIGN', (-1, -1),(-1, -1), 'RIGHT'),
        ]))
        return table

    def __top_table(self, t0, t1, t3):
        cont = [[t0, t1, t3]]
        table = Table(cont,
            [
                5.5 * cm,
                9.4 * cm,
                5.5 * cm
            ]
        )
        table.setStyle( TableStyle([
            ('ALIGN', (0, 0),(0, 0), 'LEFT'),
            ('ALIGN', (1, 0),(1, 0), 'CENTRE'),
            ('ALIGN', (-1, 0),(-1, 0), 'RIGHT'),
        ]))
        return table

    def __create_emisor_table(self, dat):
        st = ParagraphStyle(
            name='info',
            fontName='Helvetica',
            fontSize=7,
            leading = 9.7 
        )

        context = {
            'inceptor': dat['INCEPTOR_NAME'],
            'rfc': dat['INCEPTOR_RFC'],
            'phone': dat['CUSTOMER_PHONE'],
            'www': dat['CUSTOMER_WWW'],
            'street': dat['INCEPTOR_STREET'],
            'number': dat['INCEPTOR_STREET_NUMBER'],
            'settlement': dat['INCEPTOR_SETTLEMENT'],
            'state': dat['INCEPTOR_STATE'].upper(),
            'town': dat['INCEPTOR_TOWN'].upper(),
            'cp': dat['INCEPTOR_CP'].upper(),
            'regimen': dat['INCEPTOR_REGIMEN'].upper(),
            'op': dat['CFDI_ORIGIN_PLACE'].upper(),
            'fontSize': '7',
            'fontName':'Helvetica',
        }

        text = Paragraph(
            '''
            <para align=center spaceb=3>
                <font name=%(fontName)s size=10 >
                    <b>%(inceptor)s</b>
                </font>
                <br/>
                <font name=%(fontName)s size=%(fontSize)s >
                    <b>RFC: %(rfc)s</b>
                </font>
                <br/>
                <font name=%(fontName)s size=%(fontSize)s >
                    <b>DOMICILIO FISCAL</b>
                </font>
                <br/>
                    %(street)s %(number)s %(settlement)s
                <br/>
                    %(town)s, %(state)s C.P. %(cp)s
                <br/>
                    TEL./FAX. %(phone)s
                <br/>
                    %(www)s
                <br/>
                    %(regimen)s
                <br/><br/>
                <b>LUGAR DE EXPEDICIÓN</b>
                <br/>
                %(op)s
            </para>
            ''' % context, st)

        cont = [[text]]
        table = Table(cont, colWidths = [ 9.0 *cm])
        table.setStyle(TableStyle(
            [('VALIGN',(-1,-1),(-1,-1),'TOP')]))
        return table

    def __create_factura_table(self, dat):
        st = ParagraphStyle(
            name='info',
            fontName='Helvetica',
            fontSize=7,
            leading = 8)

        serie_folio = "%s%s" % (dat['CFDI_SERIE'], dat['CFDI_FOLIO'])
        cont = []
        cont.append([ dat['CAP_LOADED']['TL_DOC_NAME'] ])
        cont.append(['No.' ])
        cont.append([ serie_folio ])
        cont.append([ dat['CAP_LOADED']['TL_DOC_DATE'] ])
        cont.append([ dat['CFDI_DATE'] ])
        cont.append(['FOLIO FISCAL'])
        cont.append([ Paragraph( dat['UUID'], st ) ])
        cont.append(['NO. CERTIFICADO'])
        cont.append([ dat['CFDI_CERT_NUMBER'] ])

        table = Table(cont,
            [
               5  * cm,
            ],
            [
                0.40 * cm,
                0.37* cm,
                0.37 * cm,
                0.38 * cm,
                0.38 * cm,
                0.38 * cm,
                0.70 * cm,
                0.38 * cm,
                0.38 * cm,
            ] # rowHeights
        )

        table.setStyle( TableStyle([

            #Body and header look and feel (common)
            ('BOX', (0, 1), (-1, -1), 0.25, colors.black),
            ('FONT', (0, 0), (0, 0), 'Helvetica-Bold', 10),

            ('TEXTCOLOR', (0, 1),(-1, 1), colors.white),
            ('FONT', (0, 1), (-1, 2), 'Helvetica-Bold', 7),

            ('TEXTCOLOR', (0, 3),(-1, 3), colors.white),
            ('FONT', (0, 3), (-1, 3), 'Helvetica-Bold', 7),
            ('FONT', (0, 4), (-1, 4), 'Helvetica', 7),

            ('TEXTCOLOR', (0, 5),(-1, 5), colors.white),
            ('FONT', (0, 5), (-1, 5), 'Helvetica-Bold', 7),

            ('FONT', (0, 7), (-1, 7), 'Helvetica-Bold', 7),
            ('TEXTCOLOR', (0, 7),(-1, 7), colors.white),
            ('FONT', (0, 8), (-1, 8), 'Helvetica', 7),

            ('ROWBACKGROUNDS', (0, 1),(-1, -1), [colors.black, colors.white]),
            ('ALIGN', (0, 0),(-1, -1), 'CENTER'),
            ('VALIGN', (0, 1),(-1, -1), 'MIDDLE'),
        ]))
        return table

    def __comments_table(self, dat):
        if not dat['OBSERVACIONES']:
            return None

        st = ParagraphStyle(name='info',fontName='Helvetica', fontSize=7, leading = 8)
        cont = [[ dat['CAP_LOADED']['TL_DOC_OBS'] ]]

        cont.append([ Paragraph( dat['OBSERVACIONES'], st) ])

        table = Table(cont,
            [
                20.0 * cm
            ]
        )

        table.setStyle( TableStyle([
            ('BOX', (0, 0), (-1, -1), 0.25, colors.black),
            ('VALIGN', (0, 0),(0, 0), 'MIDDLE'),
            ('ALIGN', (0, 0),(0, 0), 'LEFT'),
            ('FONT', (0, 0), (0, 0), 'Helvetica-Bold', 7),
            ('BACKGROUND', (0, 0),(0, 0), colors.black),
            ('TEXTCOLOR', (0, 0),(0, 0), colors.white),

        ]))

        return table

    def __info_cert_table(self, dat):
        cont = [['INFORMACIÓN DEL TIMBRE FISCAL DIGITAL']]
        st = ParagraphStyle(name='info',fontName='Helvetica', fontSize=6.5, leading = 8)
        table = Table(cont,
            [
                20.0 * cm
            ],
            [
                0.50*cm,
            ]
        )

        table.setStyle( TableStyle([
            ('BOX', (0, 0), (0, 0), 0.25, colors.black),
            ('VALIGN', (0, 0),(0, 0), 'MIDDLE'),
            ('ALIGN', (0, 0),(0, 0), 'LEFT'),
            ('FONT', (0, 0), (0, 0), 'Helvetica-Bold', 7),
            ('BACKGROUND', (0, 0),(0, 0), colors.black),
            ('TEXTCOLOR', (0, 0),(0, 0), colors.white)
        ]))

        return table

    def format_wrt(self, output_file, dat):
        doc = BaseDocTemplate(
            output_file, pagesize=letter,
            rightMargin=30,leftMargin=30, topMargin=30,bottomMargin=18,
        )

        story = []

        logo = Image(dat['LOGO'])
        logo.drawHeight = 3.8*cm
        logo.drawWidth = 5.2*cm

        cedula = Image(dat['CEDULA'])
        cedula.drawHeight = 3.2*cm
        cedula.drawWidth = 3.2*cm

        story.append(
            self.__top_table(
                logo,
                self.__create_emisor_table(dat),
                self.__create_factura_table(dat)
            )
        )
        story.append(Spacer(1, 0.4 * cm))
        story.append(
            self.__customer_table(
                self.__create_customer_sec(dat),
                self.__create_extra_sec(dat)
            )
        )
        story.append(Spacer(1, 0.4 * cm))
        story.append(self.__create_arts_section(dat))
        story.append(
            self.__amount_table(
                self.__create_letra_section(dat),
                self.__create_total_section(dat)
            )
        )
        story.append(Spacer(1, 0.45 * cm))

        ct = self.__comments_table(dat)
        if ct:
            story.append(ct)

        story.append(Spacer(1, 0.6* cm))
        story.append(self.__info_cert_table(dat))
        story.append(
            self.__info_stamp_table(
                cedula,
                self.__create_seals_table(dat)
            )
        )
        story.append(self.__info_cert_extra(dat))
        story.append(Spacer(1, 0.6 * cm))

        lt = self.__legend_table(dat)
        if lt:
            story.append(lt)

        def fp_foot(c, d):
            c.saveState()
            width, height = letter
            c.setFont('Helvetica',7)
            c.drawCentredString(width / 2.0, (1.00*cm), dat['FOOTER_ABOUT'])
            c.restoreState()

        bill_frame = Frame(
            doc.leftMargin, doc.bottomMargin, doc.width, doc.height,
            id='bill_frame'
        )

        doc.addPageTemplates(
            [
                PageTemplate(id='biil_page',frames=[bill_frame],onPage=fp_foot),
            ]
        )
        doc.build(story, canvasmaker=self.NumberedCanvas)
        return


    def data_acq(self, conn, d_rdirs, **kwargs):
        pass

    def data_rel(self, dat):
        pass

    class NumberedCanvas(canvas.Canvas):

        def __init__(self, *args, **kwargs):
            canvas.Canvas.__init__(self, *args, **kwargs)
            self._saved_page_states = []

        def showPage(self):
            self._saved_page_states.append(dict(self.__dict__))
            self._startPage()

        def save(self):
            """add page info to each page (page x of y)"""
            num_pages = len(self._saved_page_states)
            for state in self._saved_page_states:
                self.__dict__.update(state)
                self.draw_page_number(num_pages)
                canvas.Canvas.showPage(self)
            canvas.Canvas.save(self)

        def draw_page_number(self, page_count):
            width, height = letter
            self.setFont("Helvetica", 7)
            self.drawCentredString(width / 2.0, 0.65*cm,
                "Pagina %d de %d" % (self._pageNumber, page_count))
