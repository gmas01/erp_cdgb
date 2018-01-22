/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;

import com.agnux.cfd.v2.Base64Coder;
import com.agnux.cfd.v2.BeanFacturador;
import com.agnux.cfdi.BeanCancelaCfdi;
import com.agnux.cfdi.BeanFacturadorCfdi;
import com.agnux.cfdi.BeanFromCfdiXml;
import com.agnux.cfdi.LegacyRequest;
import com.agnux.cfdi.timbre.BeanFacturadorCfdiTimbre;
import com.agnux.common.helpers.StringHelper;
import com.agnux.common.obj.DataPost;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.FacturasInterfaceDao;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import com.agnux.tcp.BbgumProxy;
import com.agnux.tcp.BbgumProxyError;
import com.maxima.bbgum.ServerReply;
import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


@Controller
@SessionAttributes({"user"})
@RequestMapping("/notascredito/")
public class NotasCreditoController {
    ResourceProject resource = new ResourceProject();
    private static final Logger log  = Logger.getLogger(NotasCreditoController.class.getName());
    
    @Autowired
    @Qualifier("daoGral")
    private GralInterfaceDao gralDao;
    
    @Autowired
    @Qualifier("daoFacturas")
    private FacturasInterfaceDao facdao;
    
    @Autowired
    @Qualifier("beanFacturador")
    BeanFacturador bf;
    
    @Autowired
    @Qualifier("daoHome")
    private HomeInterfaceDao HomeDao;
    
    @Autowired
    @Qualifier("beanFacturadorCfdi")
    BeanFacturadorCfdi bfcfdi;
    
    @Autowired
    @Qualifier("beanCancelaCfdi")
    BeanCancelaCfdi bcancelafdi;
    
    @Autowired
    @Qualifier("beanFacturadorCfdiTf")
    BeanFacturadorCfdiTimbre bfcfditf;
    
    public BeanFacturadorCfdiTimbre getBfcfditf() {
        return bfcfditf;
    }

    public void setBfcfditf(BeanFacturadorCfdiTimbre bfcfditf) {
        this.bfcfditf = bfcfditf;
    }
    
    public BeanCancelaCfdi getBcancelafdi() {
        return bcancelafdi;
    }
    
    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }
    
    public GralInterfaceDao getGralDao() {
        return gralDao;
    }
    
    public FacturasInterfaceDao getFacdao() {
        return facdao;
    }
    
    public BeanFacturador getBf() {
        return bf;
    }
    
    public BeanFacturadorCfdi getBfcfdi() {
        return bfcfdi;
    }
    
    @RequestMapping(value="/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response, 
            @ModelAttribute("user") UserSessionData user
            )throws ServletException, IOException {
        
        log.log(Level.INFO, "Ejecutando starUp de {0}", NotasCreditoController.class.getName());
        LinkedHashMap<String,String> infoConstruccionTabla = new LinkedHashMap<String,String>();
        
        infoConstruccionTabla.put("id", "Acciones:70");
        infoConstruccionTabla.put("folio", "Folio:80");
        infoConstruccionTabla.put("cliente", "Cliente:300");
        infoConstruccionTabla.put("total", "Monto:90");
        infoConstruccionTabla.put("fecha_expedicion","Fecha Expedici&oacute;n:110");
        infoConstruccionTabla.put("moneda", "Moneda:60");
        infoConstruccionTabla.put("factura", "Factura:80");
        infoConstruccionTabla.put("estado", "Estado:90");
        
        ModelAndView x = new ModelAndView("notascredito/startup", "title", "Notas de Cr&eacute;dito");
        
        x = x.addObject("layoutheader", resource.getLayoutheader());
        x = x.addObject("layoutmenu", resource.getLayoutmenu());
        x = x.addObject("layoutfooter", resource.getLayoutfooter());
        x = x.addObject("grid", resource.generaGrid(infoConstruccionTabla));
        x = x.addObject("url", resource.getUrl(request));
        x = x.addObject("username", user.getUserName());
        x = x.addObject("empresa", user.getRazonSocialEmpresa());
        x = x.addObject("sucursal", user.getSucursal());
        
        String userId = String.valueOf(user.getUserId());
        
        //System.out.println("id_de_usuario: "+userId);
        
        String codificado = Base64Coder.encodeString(userId);
        
        //id de usuario codificado
        x = x.addObject("iu", codificado);
        
        return x;
    }
    
    
    
    
    @RequestMapping(value="/getAllNotasCredito.json", method = RequestMethod.POST)
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getAllNotasCreditoJson(
           @RequestParam(value="orderby", required=true) String orderby,
           @RequestParam(value="desc", required=true) String desc,
           @RequestParam(value="items_por_pag", required=true) int items_por_pag,
           @RequestParam(value="pag_start", required=true) int pag_start,
           @RequestParam(value="display_pag", required=true) String display_pag,
           @RequestParam(value="input_json", required=true) String input_json,
           @RequestParam(value="cadena_busqueda", required=true) String cadena_busqueda,
           @RequestParam(value="iu", required=true) String id_user_cod,
       Model modcel) {
        
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String,String> has_busqueda = StringHelper.convert2hash(StringHelper.ascii2string(cadena_busqueda));
        
        //aplicativo Notas de Credito
        Integer app_selected = 70;
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        
        //variables para el buscador
        String folio = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("folio")))+"%";
        String cliente = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("cliente")))+"%";
        String factura = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("factura")))+"%";
        String fecha_inicial = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_inicial")))+"";
        String fecha_final = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_final")))+"";
        
        String data_string = app_selected+"___"+id_usuario+"___"+folio+"___"+cliente+"___"+fecha_inicial+"___"+fecha_final+"___"+factura;
        
        //obtiene total de registros en base de datos, con los parametros de busqueda
        int total_items = this.getFacdao().countAll(data_string);
        
        //calcula el total de paginas
        int total_pags = resource.calculaTotalPag(total_items,items_por_pag);
        
        //variables que necesita el datagrid, para no tener que hacer uno por cada aplicativo
        DataPost dataforpos = new DataPost(orderby, desc, items_por_pag, pag_start, display_pag, input_json, cadena_busqueda,total_items,total_pags, id_user_cod);
        
        int offset = resource.__get_inicio_offset(items_por_pag, pag_start);
        
        //obtiene los registros para el grid, de acuerdo a los parametros de busqueda
        jsonretorno.put("Data", this.getFacdao().getNotasCredito_PaginaGrid(data_string, offset, items_por_pag, orderby, desc));
        //obtiene el hash para los datos que necesita el datagrid
        jsonretorno.put("DataForGrid", dataforpos.formaHashForPos(dataforpos));
        
        return jsonretorno;
    }
    
    
    
    
    
    //Buscador de clientes
    @RequestMapping(method = RequestMethod.POST, value="/get_buscador_clientes.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> get_buscador_clientesJson(
            @RequestParam(value="cadena", required=true) String cadena,
            @RequestParam(value="filtro", required=true) Integer filtro,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
       
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("clientes", this.getFacdao().getBuscadorClientes(cadena,filtro,id_empresa, id_sucursal));
        
        return jsonretorno;
    }
    
    
    
    //Obtener datos del cliente a partir del Numero de Control
    @RequestMapping(method = RequestMethod.POST, value="/getDataByNoClient.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getDataByNoClientJson(
            @RequestParam(value="no_control", required=true) String no_control,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
       
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("Cliente", this.getFacdao().getDatosClienteByNoCliente(no_control, id_empresa, id_sucursal));
        
        return jsonretorno;
    }
    
    
    
    @RequestMapping(method = RequestMethod.POST, value="/getNotaCredito.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getNotaCreditoJson(
            @RequestParam(value="id_nota_credito", required=true) Integer id_nota_credito,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getNotaCreditoJson de {0}", NotasCreditoController.class.getName());
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        ArrayList<HashMap<String, Object>> datosNota = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> valorIva = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> tipoCambioActual = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> tc = new HashMap<String, Object>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Aplicativo notas de credito
        Integer app_selected = 70;
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        if( id_nota_credito != 0  ){
            datosNota = this.getFacdao().getNotasCredito_Datos(id_nota_credito);
        }
        
        valorIva= this.getFacdao().getValoriva(id_sucursal);
        tc.put("tipo_cambio", StringHelper.roundDouble(this.getFacdao().getTipoCambioActual(), 4)) ;
        tipoCambioActual.add(0,tc);
        
        jsonretorno.put("Datos", datosNota);
        jsonretorno.put("iva", valorIva);
        jsonretorno.put("Tc", tipoCambioActual);
        jsonretorno.put("Monedas", this.getFacdao().getFactura_Monedas());
        jsonretorno.put("Vendedores", this.getFacdao().getFactura_Agentes(id_empresa, id_sucursal));
        jsonretorno.put("TMov", this.getFacdao().getCtb_TiposDeMovimiento(id_empresa, app_selected));
        
        return jsonretorno;
    }
    
    
    
    
    
    //Buscador de Remisiones del Cliente
    @RequestMapping(method = RequestMethod.POST, value="/getFacturasCliente.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getFacturasClienteJson(
            @RequestParam(value="id_cliente", required=true) Integer id_cliente,
            @RequestParam(value="serie_folio", required=true) String serie_folio,
            Model model
        ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        
        jsonretorno.put("Facturas", this.getFacdao().getNotasCredito_FacturasCliente(id_cliente, serie_folio));
        
        return jsonretorno;
    }
    
    
    
    
    //Obtener datos de una factura en Especifico a partir del Serie Folio
    @RequestMapping(method = RequestMethod.POST, value="/getDatosFactura.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getDatosFacturaJson(
            @RequestParam(value="id_cliente", required=true) Integer id_cliente,
            @RequestParam(value="serie_folio", required=true) String serie_folio,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        /*
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        */
        jsonretorno.put("Factura", this.getFacdao().getNotasCredito_DatosFactura(id_cliente, serie_folio));
        
        return jsonretorno;
    }








    
    
    
    //edicion y nuevo
    @RequestMapping(method = RequestMethod.POST, value="/edit.json")
    public @ResponseBody HashMap<String, String> editJson(
            @RequestParam(value="identificador", required=true) Integer id_nota_credito,
            @RequestParam(value="id_cliente", required=true) String id_cliente,
            @RequestParam(value="id_impuesto", required=true) String id_impuesto,
            @RequestParam(value="valorimpuesto", required=true) String valor_impuesto,
            @RequestParam(value="observaciones", required=true) String observaciones,
            @RequestParam(value="select_moneda", required=true) String select_moneda,
            @RequestParam(value="select_vendedor", required=true) String select_vendedor,
            @RequestParam(value="concepto", required=true) String concepto,
            @RequestParam(value="tasa_ret_immex", required=true) String tasa_ret_immex,
            @RequestParam(value="tipo_cambio", required=true) String tipo_cambio,
            @RequestParam(value="importe", required=true) String importe,
            @RequestParam(value="impuesto", required=true) String impuesto,
            @RequestParam(value="retencion", required=true) String retencion,
            @RequestParam(value="total", required=true) String total,
            @RequestParam(value="factura", required=true) String factura,
            @RequestParam(value="generar", required=true) String generar,
            @RequestParam(value="fac_saldado", required=true) String fac_saldado,
            @RequestParam(value="select_tmov", required=false) String select_tmov,
            @ModelAttribute("user") UserSessionData user
        ) throws Exception {
            
            System.out.println("Guardar del Nota de Credito");
            HashMap<String, String> jsonretorno = new HashMap<String, String>();
            HashMap<String, String> succes = new HashMap<String, String>();
            HashMap<String, String> userDat = new HashMap<String, String>();
            
            Integer app_selected = 70;//aplicativo notas de credito
            String command_selected = "new";
            Integer id_usuario= user.getUserId();//variable para el id  del usuario
            String extra_data_array = "'sin datos'";
            String actualizo = "0";
            String valorRespuesta="";
            String msjRespuesta = "";
            String serieFolio="";
            userDat = this.getHomeDao().getUserById(id_usuario);
            Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
            Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
            
            if( id_nota_credito==0 ){
                command_selected = "new";
            }else{
                command_selected = "edit";
            }
            
            select_tmov = StringHelper.verificarSelect(select_tmov);
            
            String data_string = app_selected+"___"+command_selected+"___"+id_usuario+"___"+id_nota_credito+"___"+id_cliente+"___"+id_impuesto+"___"+valor_impuesto+"___"+observaciones.toUpperCase()+"___"+select_moneda+"___"+select_vendedor+"___"+concepto.toUpperCase()+"___"+tipo_cambio+"___"+importe+"___"+impuesto+"___"+retencion+"___"+total+"___"+factura+"___"+select_tmov;
            //System.out.println("data_string: "+data_string);
            
            succes = this.getFacdao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
            
            log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
            
            if( String.valueOf(succes.get("success")).equals("true") ){
                actualizo = this.getFacdao().selectFunctionForFacAdmProcesos(data_string, extra_data_array);
                jsonretorno.put("actualizo",String.valueOf(actualizo));
            }
            
            System.out.println("Actualizo::: "+actualizo);
            
        if (generar.equals("true")) {

            if (actualizo.equals("1")) {

                String no_id = this.getGralDao().getNoIdEmpresa(id_empresa);
                String serie = this.getGralDao().getSerieNotaCredito(id_empresa, id_sucursal);
                String folio = this.getGralDao().getFolioNotaCredito(id_empresa, id_sucursal);

                String filename = no_id + "_" + serie + folio + ".xml";

                BbgumProxy bbgumProxy = new BbgumProxy();
                LegacyRequest req = new LegacyRequest();

                req.sendTo("cxc");
                req.from("webui");
                req.action("donota");
                HashMap<String, String> kwargs = new HashMap<String, String>();
                kwargs.put("filename", filename);
                kwargs.put("usr_id", id_usuario.toString());
                kwargs.put("ncr_id", id_nota_credito.toString());
                kwargs.put("saldado", fac_saldado.toString());
                req.args(kwargs);

                try {
                    ServerReply reply = bbgumProxy.uploadBuff("localhost", 10080, req.getJson().getBytes());
                    String msg = "core reply code: " + reply.getReplyCode();
                    if (reply.getReplyCode() == 0) {
                        Logger.getLogger(NotasCreditoController.class.getName()).log(
                                Level.INFO, msg);
                        jsonretorno.put("folio", serieFolio);
                        msjRespuesta = "Se gener&oacute; la Nota de Cr&eacute;dito: " + serie + folio;
                        valorRespuesta = "true";
                    } else {
                        Logger.getLogger(NotasCreditoController.class.getName()).log(
                                Level.WARNING, msg);
                        valorRespuesta = "false";
                        msjRespuesta = msg;
                    }
                } catch (BbgumProxyError ex) {
                    Logger.getLogger(NotasCreditoController.class.getName()).log(
                            Level.WARNING, ex.getMessage());
                    valorRespuesta = "false";
                    msjRespuesta = ex.getMessage();
                }

            } else {
                if (actualizo.equals("0")) {
                    jsonretorno.put("actualizo", String.valueOf(actualizo));
                    //Aqui entra cuando No se Genera Nota de Credito, solo actualiza el registro en la tabla
                    valorRespuesta = "false";
                    msjRespuesta = "Error al actualizar los datos. Intente de nuevo.";
                }
            }

        } else {
            if (actualizo.equals("1")) {
                //Aqui entra cuando No se Genera Nota de Credito, solo actualiza el registro en la tabla
                valorRespuesta = "true";
                msjRespuesta = "Los datos se actualizaron con &eacute;xito.\nPuede proceder a generar la Nota de Cr&eacute;dito.";
            }
        }//Termina if genarar

        jsonretorno.put("success", String.valueOf(succes.get("success")));
        jsonretorno.put("valor", valorRespuesta);
        jsonretorno.put("msj", msjRespuesta);

        log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));

        System.out.println("Validacion: " + String.valueOf(jsonretorno.get("success")));
        System.out.println("Actualizo: " + String.valueOf(jsonretorno.get("actualizo")));
        System.out.println("valorRespuesta: " + String.valueOf(valorRespuesta));
        System.out.println("msjRespuesta: " + String.valueOf(msjRespuesta));
        return jsonretorno;
    }
    
    
    //Obtiene los tipos de cancelacion
    @RequestMapping(method = RequestMethod.POST, value="/getVerificaArchivoGenerado.json")
    public @ResponseBody HashMap<String,String> getVerificaArchivoGeneradoJson(
            @RequestParam(value="serie_folio", required=true) String serie_folio,
            @RequestParam(value="ext", required=true) String extension,
            @RequestParam(value="id", required=true) Integer id,
            @RequestParam(value="iu", required=true) String id_user_cod,
            Model model
        ) {
        
        log.log(Level.INFO, "Ejecutando getVerificaArchivoGeneradoJson de {0}", NotasCreditoController.class.getName());
        HashMap<String, String> jsonretorno = new HashMap<String,String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        String existe ="false";
        String dirSalidas = "";
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //Obtener tipo de facturacion
        String tipo_facturacion = this.getFacdao().getTipoFacturacion(id_empresa);
        
        if(tipo_facturacion.equals("cfd")){
            dirSalidas = this.getGralDao().getCfdEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        }else{
            if(tipo_facturacion.equals("cfditf")){
                dirSalidas = this.getGralDao().getCfdiTimbreEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
            }else{
                dirSalidas = this.getGralDao().getCfdiSolicitudesDir() + "out/";
            }
        }
        
        String nombre_archivo = this.getFacdao().getRefIdNotaCredito(id);
        
        String fileout = dirSalidas +"/"+ nombre_archivo +"."+extension;
        
        System.out.println("Ruta: " + fileout);
        File file = new File(fileout);
        if (file.exists()){
            existe="true";
        }
        
        jsonretorno.put("descargar", existe);
        
        return jsonretorno;
    }
    
    
    //Cancelacion de Notas de Crédito
    @RequestMapping(method = RequestMethod.POST, value="/cancelarNotaCredito.json")
    public @ResponseBody HashMap<String, String> cancelarNotaCreditoJson(
            @RequestParam(value="id_nota", required=true) Integer id_nota,
            @RequestParam(value="tmov", required=false) String tmov,
            @RequestParam(value="motivo", required=true) String motivo_cancelacion,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) throws IOException {
        
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        Integer id_usuario=0;//aqui va el id del usuario
        
        //decodificar id de usuario
        id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        //Aplicativo notas de credito
        Integer app_selected = 70;
        String command_selected = "cancelacion";
        String extra_data_array = "'sin datos'";
        String succcess = "false";
        String serie_folio="";
        String refId="";
        String tipo_facturacion="";
        String valorRespuesta="false";
        String msjRespuesta="";
        
        //obtener tipo de facturacion
        tipo_facturacion = this.getFacdao().getTipoFacturacion(id_empresa);
        tipo_facturacion = String.valueOf(tipo_facturacion);
        
        //Obtener el numero del PAC para el Timbrado de la Factura
        String noPac = this.getFacdao().getNoPacFacturacion(id_empresa);
        
        //Obtener el Ambiente de Facturacion PRUEBAS ó PRODUCCION, solo aplica para Facturacion por Timbre FIscal(cfditf)
        String ambienteFac = this.getFacdao().getAmbienteFacturacion(id_empresa);
        
        System.out.println("Tipo::"+tipo_facturacion+" | noPac::"+noPac+" | Ambiente::"+ambienteFac);
        
        tmov = StringHelper.verificarSelect(tmov);
        
        String data_string = app_selected+"___"+command_selected+"___"+id_usuario+"___"+id_nota+"___"+motivo_cancelacion.toUpperCase()+"___"+tipo_facturacion+"___"+tmov;
        
        if(tipo_facturacion.equals("cfdi") || tipo_facturacion.equals("cfd")){
            succcess = this.getFacdao().selectFunctionForFacAdmProcesos(data_string, extra_data_array);
        }
        
        serie_folio = this.getFacdao().getSerieFolioNotaCredito(id_nota);
        refId = this.getFacdao().getRefIdNotaCredito(id_nota);
        
        if(tipo_facturacion.equals("cfd")){
            if(succcess.split(":")[1].equals("true")){
                valorRespuesta="true";
                msjRespuesta="La Nota de Cr&eacute;dito "+serie_folio+", fue cancelada con &eacute;xito.";
            }else{
                valorRespuesta="false";
                msjRespuesta="La Nota de Cr&eacute;dito "+serie_folio+", no fue posible cancelar.\nIntente de nuevo.";
            }
        }
        
        
        
        //tipo facturacion CFDI. Generar txt para buzon fiscal
        if(tipo_facturacion.equals("cfdi")){
            
            //Pac 0=Sin PAC, 1=Diverza, 2=ServiSim
            if(!noPac.equals("0") && !noPac.equals("2")){
                //Solo se permite Cancelar Nota de Credito por Conector Fiscal con Diverza
                
                File toFile = new File(this.getGralDao().getCfdiSolicitudesDir() + "out/"+refId+".xml");
                //System.out.println("FicheroXML: "+this.getGralDao().getCfdiSolicitudesDir() + "out/"+serie_folio+".xml");
                
                if (toFile.exists()) {
                    if(succcess.split(":")[1].equals("true")){
                        HashMap<String, String> data = new HashMap<String, String>();
                        serie_folio = succcess.split(":")[0];
                        
                        String directorioSolicitudesCfdiOut=this.getGralDao().getCfdiSolicitudesDir() + "out/"+refId+".xml";
                        BeanFromCfdiXml pop = new BeanFromCfdiXml(directorioSolicitudesCfdiOut);
                        
                        data.put("uuid", pop.getUuid());
                        data.put("emisor_rfc", pop.getEmisor_rfc());
                        data.put("receptor_rfc", pop.getReceptor_rfc());
                        
                        //generar archivo de texto para cfdi
                        this.getBcancelafdi().init(data, serie_folio);
                        this.getBcancelafdi().start();
                        
                        System.out.println("serie_folio:"+serie_folio + "    Cancelado:"+succcess.split(":")[1]);
                    }else{
                        valorRespuesta="false";
                        msjRespuesta="No fue posible cancelar la Nota de Cr&eacute;dito "+serie_folio+".\nIntente de nuevo.";
                    }
                }else{                        
                    valorRespuesta="false";
                    msjRespuesta="No fue posible cancelar la Nota de Cr&eacute;dito "+serie_folio+". No se encuentra el archivo XML.";
                }
            }else{
                valorRespuesta="false";
                msjRespuesta="No se puede Cancelar la Nota de Cr&eacute;dito por Conector Fiscal con el PAC actual.\nVerifique la configuraci&oacute;n del tipo de Facturaci&oacute;n y del PAC.";
            }
        }
        
        if(tipo_facturacion.equals("cfditf")){
            
            //Pac 0=Sin PAC, 1=Diverza, 2=ServiSim
            if(!String.valueOf(noPac).equals("0")){
                //Solo se permite Cancelar Factura con Diverza y ServiSim
                
                String rfcEmpresaEmisora = this.getGralDao().getRfcEmpresaEmisora(id_empresa);
                
                String directorioEmitidosCfdiTimbre = this.getGralDao().getCfdiTimbreEmitidosDir() + rfcEmpresaEmisora +"/"+ refId+".xml";
                
                File toFile = new File(directorioEmitidosCfdiTimbre);
                //System.out.println("FicheroXML: "+directorioEmitidosCfdiTimbre);
                
                if (toFile.exists()) {
                    
                    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    //aqui inicia request al webservice
                    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
                    
                    String rutaCanceladosDir = this.getGralDao().getCfdiTimbreCanceladosDir();
                    String ruta_ejecutable_java = this.getGralDao().getJavaVmDir(id_empresa, id_sucursal);
                    //String ruta_ejecutable_java = "/home/agnux/jdk/bin/java";
                    String ruta_jarWebService = this.getGralDao().getCfdiTimbreJarWsDir()+"wscli.jar";
                    String ruta_fichero_llave_pfx = this.getGralDao().getSslDir() + rfcEmpresaEmisora+ "/" +this.getGralDao().getFicheroPfxTimbradoCfdi(id_empresa,id_sucursal) ;
                    String password_pfx = this.getGralDao().getPasswdFicheroPfxTimbradoCfdi(id_empresa, id_sucursal);
                    String ruta_java_almacen_certificados = this.getGralDao().getJavaRutaCacerts(id_empresa, id_sucursal);
                    //String ruta_ejecutable_java = "/home/agnux/jdk/bin/java";
                    
                    //Parsear el XML de la Nota de Crédito
                    BeanFromCfdiXml pop = new BeanFromCfdiXml(directorioEmitidosCfdiTimbre);
                    
                    String uuid = pop.getUuid();
                    String emisor_rfc = pop.getEmisor_rfc();
                    String receptor_rfc = pop.getReceptor_rfc();
                    String tipo_peticion = "cancelacfdi";
                    
                    String str_execute="";
                    
                    //Cancelacion con DIVERZA
                    if(String.valueOf(noPac).equals("1")){
                        /*
                        //Datos para cancelacion
                        args[0] = PAC proveedor
                        args[1] = tipo de ambiente(pruebas, produccion)
                        args[2] = tipo_peticion
                        args[3] = FicheroPfxTimbradoCfdi
                        args[4] = PasswdFicheroPfxTimbradoCfdi
                        args[5] = JavaVmDir
                        args[6] = getRfc_emisor
                        args[7] = getRfc_receptor
                        args[8] = uuid
                        args[9] = DirCancelados
                        args[10] = serie_folio
                         */
                        //str_execute = ruta_ejecutable_java+" -jar "+ruta_jarWebService+" cancelacfdi "+ruta_fichero_llave_pfx+" "+password_pfx+" "+ruta_java_almacen_certificados+" "+emisor_rfc+" "+receptor_rfc+" "+uuid;
                        str_execute = ruta_ejecutable_java+" -jar "+ruta_jarWebService+" "+noPac+" "+ambienteFac+" "+tipo_peticion+" "+ruta_fichero_llave_pfx+" "+password_pfx+" "+ruta_java_almacen_certificados+" "+emisor_rfc+" "+receptor_rfc+" "+uuid+" "+rutaCanceladosDir+" "+serie_folio;
                    }
                    
                    //Cancelacion con SERVISIM
                    if(String.valueOf(noPac).equals("2")){
                        /*
                        //Datos para Cancelacion
                        args[0] = PAC proveedor
                        args[1] = tipo de ambiente(pruebas, produccion)
                        args[2] = tipo_peticion(cancelacion, timbrado)
                        args[3] = Usuario
                        args[4] = Password
                        args[5] = uuid
                        args[6] = rfcEmisor
                        args[7] = serieFolio
                        args[8] = dirCancelados
                        */

                        String usuario = this.getGralDao().getUserContrato(id_empresa, id_sucursal);
                        String contrasena = this.getGralDao().getPasswordUserContrato(id_empresa, id_sucursal);

                        //aqui se forma la cadena con los parametros que se le pasan a jar
                        str_execute = ruta_ejecutable_java+" -jar "+ruta_jarWebService+" "+noPac+" "+ambienteFac+" "+tipo_peticion+" "+usuario+" "+contrasena+" "+uuid+" "+emisor_rfc+" "+serie_folio+" "+rutaCanceladosDir;
                    }
                    
                    System.out.println("str_execute: "+str_execute);
                    
                    Process resultado = null; 
                    resultado = Runtime.getRuntime().exec(str_execute);
                    
                    InputStream myInputStream=null;
                    myInputStream= resultado.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(myInputStream));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    myInputStream.close();
                    
                    System.out.println("Resultado: "+sb.toString());
                    String arrayResult[] = sb.toString().split("___");
                    
                    //Toma el valor true o false
                    valorRespuesta = arrayResult[0];
                    
                    //Toma el mensaje
                    msjRespuesta = arrayResult[1];
                    
                    if(String.valueOf(valorRespuesta).equals("true")){
                        succcess = this.getFacdao().selectFunctionForFacAdmProcesos(data_string, extra_data_array);
                        if(String.valueOf(succcess).equals("true")){
                            HashMap<String, String> data = new HashMap<String, String>();
                            //serie_folio = succcess.split(":")[0];
                            //System.out.println("serie_folio:"+serie_folio + "    Cancelado:"+succcess.split(":")[1]);
                        }
                    }
                    
                }else{
                    valorRespuesta="false";
                    msjRespuesta="No fue posible cancelar la Nota de Cr&eacute;dito "+serie_folio+". No se encuentra el archivo XML.";
                }
            }else{
                valorRespuesta="false";
                msjRespuesta="No se puede Cancelar la Nota de Cr&eacute;dito por Conector Fiscal con el PAC actual.\nVerifique la configuraci&oacute;n del tipo de Facturaci&oacute;n y del PAC.";
            }
        }
        
        jsonretorno.put("success", succcess);
        jsonretorno.put("valor",valorRespuesta);
        jsonretorno.put("msj",msjRespuesta);
        
        System.out.println("valorRespuesta: "+String.valueOf(valorRespuesta));
        System.out.println("msjRespuesta: "+String.valueOf(msjRespuesta));
        
        return jsonretorno;
    }
    
    
    //Obtiene los tipos de cancelacion
    @RequestMapping(method = RequestMethod.POST, value="/getDataCancel.json")
    public @ResponseBody HashMap<String,Object> getDataCancelJson(
            @RequestParam(value="identificador", required=true) Integer identificador,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        log.log(Level.INFO, "Ejecutando getDataCancelJson de {0}", NotasCreditoController.class.getName());
        HashMap<String,Object> jsonretorno = new HashMap<String,Object>();
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Cancelacion de Notas de Credito(Numero de aplicativo FALSO, Solo es para mostrar un numero en el programa de definicion de asientos)
        Integer app_selected = 2001;
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        //Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("TMov", this.getFacdao().getCtb_TiposDeMovimiento(id_empresa, app_selected));
        
        return jsonretorno;
    }
    
    
    
    //Descarga pdf de la Nota de Credito generado anteriormente
    @RequestMapping(value = "/getDescargarPdfNotaCredito/{id_nota_credito}/{iu}/out.json", method = RequestMethod.GET ) 
    public ModelAndView getDescargaPdfFacturaJson(
            @PathVariable("id_nota_credito") Integer id_nota_credito, 
            @PathVariable("iu") String id_user,
            HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException, URISyntaxException {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        String dirSalidas = "";
        String nombre_archivo = "";
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        
        //obtener tipo de facturacion
        String tipo_facturacion = this.getFacdao().getTipoFacturacion(id_empresa);
        
        if(tipo_facturacion.equals("cfd")){
            dirSalidas = this.getGralDao().getCfdEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        }else{
            if(tipo_facturacion.equals("cfditf")){
                dirSalidas = this.getGralDao().getCfdiTimbreEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
            }else{
                dirSalidas = this.getGralDao().getCfdiSolicitudesDir() + "out/";
            }
        }
        
        nombre_archivo = this.getFacdao().getRefIdNotaCredito(id_nota_credito);
        
        
        String fileout = dirSalidas + "/" + nombre_archivo +".pdf";
        
        System.out.println("Recuperando archivo: " + fileout);
        File file = new File(fileout);
        int size = (int) file.length(); // Tamaño del archivo
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        response.setBufferSize(size);
        response.setContentLength(size);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
        FileCopyUtils.copy(bis, response.getOutputStream());  	
        response.flushBuffer();
        
        return null;
        
    }
    
    
    
    
    
    //Descarga xml de la Nota de Credito
    @RequestMapping(value = "/getDescargarXmlNotaCredito/{id_nota_credito}/{iu}/out.json", method = RequestMethod.GET ) 
    public ModelAndView getDescargaXmlFacturaJson(
            @PathVariable("id_nota_credito") Integer id_nota_credito, 
            @PathVariable("iu") String id_user,
            HttpServletRequest request, 
            HttpServletResponse response, 
            Model model) throws ServletException, IOException, URISyntaxException {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        String dirSalidas = "";
        String nombre_archivo = "";
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        
        
        //obtener tipo de facturacion
        String tipo_facturacion = this.getFacdao().getTipoFacturacion(id_empresa);
        
        if(tipo_facturacion.equals("cfd")){
            dirSalidas = this.getGralDao().getCfdEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        }else{
            if(tipo_facturacion.equals("cfditf")){
                dirSalidas = this.getGralDao().getCfdiTimbreEmitidosDir() + this.getGralDao().getRfcEmpresaEmisora(id_empresa);
            }else{
                dirSalidas = this.getGralDao().getCfdiSolicitudesDir() + "out/";
            }
        }
        
        nombre_archivo = this.getFacdao().getRefIdNotaCredito(id_nota_credito);
        
        //ruta completa del archivo a descargar
        String fileout = dirSalidas + "/" + nombre_archivo +".xml";
        
        
        
        //System.out.println("Recuperando archivo: " + fileout);
        File file = new File(fileout);
        
        if (file.exists()){
            int size = (int) file.length(); // Tamaño del archivo
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            response.setBufferSize(size);
            response.setContentLength(size);
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
            FileCopyUtils.copy(bis, response.getOutputStream());  	
            response.flushBuffer();
            
        }
     
        return null;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
