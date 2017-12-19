/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;

import com.agnux.cfd.v2.Base64Coder;
import com.agnux.common.helpers.FileHelper;
import com.agnux.common.helpers.StringHelper;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.PocInterfaceDao;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import com.agnux.kemikal.reportes.PdfReportedePedidos;
import com.itextpdf.text.DocumentException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * /**
 *
 * @author
 * 06-07-2012
 * Este controller es para generar el reporte de Pedidos
 */

@Controller
@SessionAttributes({"user"})
@RequestMapping("/facglobal2/")

public class FacGlobalController2 {
    private static final Logger log  = Logger.getLogger(FacGlobalController2.class.getName());
    ResourceProject resource = new ResourceProject();
    
    @Autowired
    @Qualifier("daoPoc")
    private PocInterfaceDao PedDao;
        
    
    @Autowired
    @Qualifier("daoHome")
    private HomeInterfaceDao HomeDao;
    
    @Autowired
    @Qualifier("daoGral")
    private GralInterfaceDao gralDao;
    private Integer agente_id;
    
    public PocInterfaceDao getPedidDao() {
        return PedDao;
    }
    
    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }
    
    
    public GralInterfaceDao getGralDao() {
        return gralDao;
    }
    
    
    
    @RequestMapping(value="/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response, 
            @ModelAttribute("user") UserSessionData user)
            throws ServletException, IOException {
        
        log.log(Level.INFO, "Ejecutando starUp de {0}", FacGlobalController2.class.getName());
        LinkedHashMap<String,String> infoConstruccionTabla = new LinkedHashMap<String,String>();
        
        
        ModelAndView x = new ModelAndView("facglobal2/startup", "title", "Factura Global");
        
        x = x.addObject("layoutheader", resource.getLayoutheader());
        x = x.addObject("layoutmenu", resource.getLayoutmenu());
        x = x.addObject("layoutfooter", resource.getLayoutfooter());
        x = x.addObject("grid", resource.generaGrid(infoConstruccionTabla));
        x = x.addObject("url", resource.getUrl(request));
        x = x.addObject("username", user.getUserName());
        x = x.addObject("empresa", user.getRazonSocialEmpresa());
        x = x.addObject("sucursal", user.getSucursal());
        
        String userId = String.valueOf(user.getUserId());
        
        String codificado = Base64Coder.encodeString(userId);
        
        //id de usuario codificado
        x = x.addObject("iu", codificado);
        
        return x;
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
        //System.out.println("id_usuario: "+id_usuario);
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        String permite_descto="false";
                
        jsonretorno.put("Clientes", this.getPedidDao().getBuscadorClientes(cadena,filtro,id_empresa, id_sucursal, permite_descto));
        
        return jsonretorno;
    }
    
    //obtiene los tipos de agente
    @RequestMapping(method = RequestMethod.POST, value="/getBuscaDatos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getAgentesJson(
        @RequestParam(value="iu", required=true) String id_user,Model model){

        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();

        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        jsonretorno.put("agentes", this.getPedidDao().getAgente(id_empresa));
        jsonretorno.put("proceso", this.getPedidDao().getEstadoPedido());
        
        return jsonretorno;
    
    }
    
    
    //obtiene la existencia de un Almacen en especifico
    @RequestMapping(method = RequestMethod.POST, value="/getPedidosCaja.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getPedidosCajaJson(
            @RequestParam("opcion") Integer opcion,
            @RequestParam("agente") Integer agente,
            @RequestParam("cliente") String cliente,
            @RequestParam("fecha_inicial") String fecha_inicial,
            @RequestParam("fecha_final") String fecha_final,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getPedidosCajaJson de {0}", FacGlobalController2.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        
 
        
        ArrayList<HashMap<String, String>> pedidos = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>>  totales = new ArrayList<HashMap<String, String>>();
        LinkedHashMap<String,String> total = new LinkedHashMap<String,String>();        
        HashMap<String, String> userDat = new HashMap<String, String>();
        Double suma_pesos_subtotal = 0.0;
        Double suma_pesos_monto_ieps = 0.0;
        Double suma_pesos_impuesto = 0.0;
        Double suma_pesos_total = 0.0;
        Double suma_dolares_subtotal = 0.0;
        Double suma_dolares_monto_ieps = 0.0;
        Double suma_dolares_impuesto = 0.0;
        Double suma_dolares_total = 0.0;
        
        Double suma_pesos_efectivo = 0.0;
        Double suma_pesos_transferencia = 0.0;
        Double suma_pesos_amex = 0.0;
        Double suma_pesos_tcredito = 0.0;
        Double suma_pesos_tdebito = 0.0;
        Double suma_pesos_otros = 0.0;
        
        Double suma_dolares_efectivo = 0.0;
        Double suma_dolares_transferencia = 0.0;
        Double suma_dolares_amex = 0.0;
        Double suma_dolares_tcredito = 0.0;
        Double suma_dolares_tdebito = 0.0;
        Double suma_dolares_otros = 0.0;
        
        Double suma_efectivo= 0.0;
        Double suma_transferencia = 0.0;
        Double suma_amex = 0.0;
        Double suma_tcredito = 0.0;
        Double suma_tdebito = 0.0;
        Double suma_otros = 0.0;
        
        Double suma_subtotal_mn = 0.0;
        Double suma_monto_ieps_mn = 0.0;
        Double suma_impuesto_mn = 0.0;
        Double suma_total_mn = 0.0;
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
       //GMAS16102017 Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        pedidos = this.getPedidDao().getReportePedidosCaja(opcion,agente, cliente.toUpperCase(), fecha_inicial, fecha_final,id_sucursal);
        
        
        for (int x=0; x<=pedidos.size()-1;x++){
            HashMap<String,String> registro = pedidos.get(x);
            //sumar cantidades
            if(registro.get("moneda_factura").equals("M.N.")){
                suma_pesos_subtotal += Double.parseDouble(registro.get("subtotal"));
                suma_pesos_monto_ieps += Double.parseDouble(registro.get("monto_ieps"));
                suma_pesos_impuesto += Double.parseDouble(registro.get("impuesto"));
                suma_pesos_total += Double.parseDouble(registro.get("total"));
                
                suma_pesos_efectivo += Double.parseDouble(registro.get("efectivo"));
                suma_pesos_transferencia += Double.parseDouble(registro.get("transfer"));
                suma_pesos_amex += Double.parseDouble(registro.get("amex"));
                suma_pesos_tcredito += Double.parseDouble(registro.get("tcredito"));
                suma_pesos_tdebito += Double.parseDouble(registro.get("tdebito"));
                suma_pesos_otros += Double.parseDouble(registro.get("otros"));
            }
            
            if(registro.get("moneda_factura").equals("USD")){
                suma_dolares_subtotal += Double.parseDouble(registro.get("subtotal"));
                suma_dolares_monto_ieps += Double.parseDouble(registro.get("monto_ieps"));
                suma_dolares_impuesto += Double.parseDouble(registro.get("impuesto"));
                suma_dolares_total += Double.parseDouble(registro.get("total"));
                
            //    suma_dolares_efectivo += Double.parseDouble(registro.get("efectivo"));
            //    suma_dolares_transferencia += Double.parseDouble(registro.get("transferencia"));
            //    suma_dolares_amex += Double.parseDouble(registro.get("amex"));
            //    suma_dolares_tcredito += Double.parseDouble(registro.get("tcredito"));
            //    suma_dolares_tdebito += Double.parseDouble(registro.get("tdebito"));
            //    suma_dolares_otros += Double.parseDouble(registro.get("otros"));
            }
            
            suma_subtotal_mn += Double.parseDouble(registro.get("subtotal_mn"));
            suma_monto_ieps_mn += Double.parseDouble(registro.get("monto_ieps_mn"));
            suma_impuesto_mn += Double.parseDouble(registro.get("impuesto_mn"));
            suma_total_mn += Double.parseDouble(registro.get("total_mn"));
            
     //       suma_efectivo += Double.parseDouble(registro.get("suma_efectivo"));
     //       suma_transferencia += Double.parseDouble(registro.get("suma_transferencia"));
     //       suma_amex += Double.parseDouble(registro.get("suma_amex"));
     //       suma_tcredito += Double.parseDouble(registro.get("suma_tcredito"));
     //       suma_tdebito += Double.parseDouble(registro.get("suma_tdebito"));
     //       suma_otros += Double.parseDouble(registro.get("suma_otros"));
            
        }
        
        total.put("suma_pesos_subtotal", StringHelper.roundDouble(suma_pesos_subtotal,2));
        total.put("suma_pesos_impuesto", StringHelper.roundDouble(suma_pesos_impuesto,2));
        total.put("suma_pesos_total", StringHelper.roundDouble(suma_pesos_total,2));
        total.put("suma_dolares_subtotal", StringHelper.roundDouble(suma_dolares_subtotal,2));
        total.put("suma_dolares_impuesto", StringHelper.roundDouble(suma_dolares_impuesto,2));
        total.put("suma_dolares_total", StringHelper.roundDouble(suma_dolares_total,2));
        total.put("suma_subtotal_mn", StringHelper.roundDouble(suma_subtotal_mn,2));
        total.put("suma_impuesto_mn", StringHelper.roundDouble(suma_impuesto_mn,2));
        total.put("suma_total_mn", StringHelper.roundDouble(suma_total_mn,2));
        total.put("suma_pesos_monto_ieps", StringHelper.roundDouble(suma_pesos_monto_ieps,2));
        total.put("suma_dolares_monto_ieps", StringHelper.roundDouble(suma_dolares_monto_ieps,2));
        total.put("suma_monto_ieps_mn", StringHelper.roundDouble(suma_monto_ieps_mn,2));
        
        total.put("suma_pesos_efectivo", StringHelper.roundDouble(suma_pesos_efectivo,2));
        total.put("suma_pesos_transferencia", StringHelper.roundDouble(suma_pesos_transferencia,2));
        total.put("suma_pesos_amex", StringHelper.roundDouble(suma_pesos_amex,2));
        total.put("suma_pesos_tcredito", StringHelper.roundDouble(suma_pesos_tcredito,2));
        total.put("suma_pesos_tdebito", StringHelper.roundDouble(suma_pesos_tdebito,2));
        total.put("suma_pesos_otros", StringHelper.roundDouble(suma_pesos_otros,2));
   //     total.put("suma_efectivo", StringHelper.roundDouble(suma_efectivo,2));
   //     total.put("suma_transferencia", StringHelper.roundDouble(suma_transferencia,2));
   //     total.put("suma_amex", StringHelper.roundDouble(suma_amex,2));
   //     total.put("suma_tcredito", StringHelper.roundDouble(suma_tcredito,2));
   //     total.put("suma_tdebito", StringHelper.roundDouble(suma_tdebito,2));
   //     total.put("suma_otros", StringHelper.roundDouble(suma_otros,2));
        
        
        
        totales.add(total);
        
        jsonretorno.put("Pedidos", pedidos);
        jsonretorno.put("Totales", totales);
        
        log.log(Level.INFO, "Ejecutando 55 getPedidosCajaJson de {0}");
        
        return jsonretorno;
        
    }
    
    
  
    
     //PDF reporte de Pedidos
     @RequestMapping(value = "/Maker_PDF_Pedidos/{cadena}/{iu}/out.json", method = RequestMethod.GET ) 
     public ModelAndView Maker_PDF_Pedidos(
        @PathVariable("cadena") String cadena,
        @PathVariable("iu") String id_user,
        HttpServletRequest request,
        HttpServletResponse response, 
        Model model)   
     throws ServletException, IOException, URISyntaxException, DocumentException {
       
        String[] datitos = cadena.split("___");
        Integer opcion = Integer.parseInt(datitos[0]);
        Integer agente = Integer.parseInt(datitos[1]);
        String  cliente = datitos[2];
        String fecha_inicial = datitos[3];
        String fecha_final = datitos[4];
        
        
        HashMap<String, String> userDat = new HashMap<String, String>();        
        System.out.println("Generando Reporte de Remisiones");        
        String dir_tmp = this.getGralDao().getTmpDir();        
        File file_dir_tmp = new File(dir_tmp);
        String file_name = "RepPedidosCaja"+opcion+"_"+cliente+".pdf";
        
        //ruta de archivo de salida
        String fileout = file_dir_tmp +"/"+  file_name;
        
        ArrayList<HashMap<String, String>> pedidos = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> datosEncabezadoPie = new HashMap<String, String>();
        HashMap<String, String> datos= new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        String rfc_empresa=this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        String razon_social_empresa = this.getGralDao().getRazonSocialEmpresaEmisora(id_empresa);
        
        String titulo_reporte ="Reporte de Pedidos";
        datosEncabezadoPie.put("nombre_empresa_emisora", razon_social_empresa);
        datosEncabezadoPie.put("titulo_reporte", titulo_reporte);
        datosEncabezadoPie.put("codigo1", "");
        datosEncabezadoPie.put("codigo2","");
        datos.put("fecha_inicial",fecha_inicial);
        datos.put("fecha_final",fecha_final);
        
        //obtiene los depositos del periodo indicado
//        String codigo="";
//        String descripcion="";
//        Integer idcliente = Integer.parseInt(cliente);
       //(HashMap<String, String> datosEncabezadoPie, String fileout, ArrayList<HashMap<String, String>> lista_CobranzaDiaria, HashMap<String, String> datos)
        //pedidos = this.getPedidDao().getReportePedidos(arreglo[0],arreglo[1], arreglo[2], arreglo[3], arreglo[4],id_empresa);
        pedidos = this.getPedidDao().getReportePedidos(opcion,agente, cliente.toUpperCase(), fecha_inicial, fecha_final,id_empresa);
        //getPedDaoDao().getCobranzaDiaria( arreglo[1], arreglo[2],idcliente,  id_empresa);
        
        //instancia a la clase que construye el pdf de Cobranza Diaria
        PdfReportedePedidos x = new PdfReportedePedidos(datosEncabezadoPie, fileout,pedidos,datos);
        
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
        try {
            FileHelper.delete(fileout);
        } catch (Exception ex) {
            Logger.getLogger(FacGlobalController2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        
    } 
    
}
