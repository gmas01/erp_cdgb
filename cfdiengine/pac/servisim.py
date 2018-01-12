from pac.adapter import Adapter, AdapterError
from suds.client import Client
from suds.transport import TransportError
import suds
import urllib.error


class Servisim(Adapter):
    """
    Current WS API of PAC Servisim
    """
    __PAC_DESC = 'Servisim - Facturacion Electronica'

    def __init__(self, logger, **kwargs):
        super().__init__(logger, self.__PAC_DESC)
        self.config = {
            'EP': kwargs.get('end_point', None),
            'LOGIN': kwargs.get('login', None),
            'PASSWD': kwargs.get('passwd', None),
        }

    def __setup_req(self, m):

        def connect():
            try:
                connection = Client(self.config['EP'])
                self.logger.debug(
                    "{0} adapter is up and ready to kick buttocks\n{1}".format(
                        self.pac_name, connection))
                return connection
            except (TransportError, urllib.error.HTTPError) as e:
                raise AdapterError(
                    'can not connect with end point {}: {}'.format(
                        self.config['EP'], e)
                )

        conn = connect()
        req = conn.factory.create(m)
        req.User = self.config['LOGIN']
        req.Pass = self.config['PASSWD']
        return req, conn

    def __check(self, r, usage):
        try:
            self.logger.info('Code {} received from PAC'.format(r['Codigo']))
            if r['Codigo'] != 0:
                raise AdapterError(
                    "{} experimenting problems: {} ({})".format(usage, r['Descripcion'], r['Codigo']))
            return r['Xml']
        except KeyError:
            raise AdapterError('unexpected format of PAC reply')


    def stamp(self, xml, xid):
        """
        Timbrado usando XML firmado por el cliente
        Args:
            xml (str): xml de cfdi firmado por cliente
            xid (str): mi identificador alternativo de cfdi
        """
        try:
            req, conn = self.__setup_req('ns0:TimbradoCFDIRequest')
            req.TipoPeticion = '1'  # SIGNED BY CUSTOMER
            req.IdComprobante = xid
            req.Xml = xml
            self.logger.debug(
                "The following request for stamp will be sent\n{0}".format(req)
            )
            return self.__check(conn.service.timbrarCFDI(req), 'Stamp')
        except AdapterError:
            raise
        except (suds.WebFault, Exception) as e:
            raise AdapterError("Stamp experimenting problems: {}".format(e))

    def fetch(self, xid):
        """
        Obtencion de cfdi previamente timbrado mediante
        identificador de cfdi
        Args:
            xid (str): mi identificador alternativo de cfdi
        """
        try:
            req, conn = self.__setup_req('ns0:ObtencionCFDIRequest')
            req.TipoPeticion = '2'  # TO EXPECT CFDI ALTERNATIVE ID
            req.Emisor = self.config['RFC']
            req.Identificador = xid
            self.logger.debug(
                "The following request for fetch will be sent\n{0}".format(req)
            )
            return conn.service.obtenerCFDI(req)
        except (suds.WebFault, Exception) as e:
            raise AdapterError("Fetch experimenting problems: {}".format(e))

    def cancel(self, xml, emisor):
        """
        Cancelacion de XML firmado por el cliente
        Args:
            xml (str): xml de cfdi firmado por cliente
        """
        try:
            req, conn = self.__setup_req('ns0:CancelacionCFDIRequest')
            req.TipoPeticion = '2'
            req.Emisor = emisor     # RFC
            req.Xml = xml
            self.logger.debug(
                "The following request for cancel will be sent\n{0}".format(req))
            return self.__check(conn.service.cancelarCFDI(req), 'Cancel')
        except (suds.WebFault, Exception) as e:
            raise AdapterError("Cancel experimenting problems".format(e))
