/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;

import com.agnux.cfd.v2.Base64Coder;
import com.agnux.common.helpers.StringHelper;
import com.agnux.common.obj.DataPost;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.ComInterfaceDao;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 *
 * @author Valentin Santos
 * valentin.vale@gmail.com
 * 27/Agosto/2012
 */
@Controller
@SessionAttributes({"user"})
@RequestMapping("/com_requisicion/")
public class ComRequisicionController {
    ResourceProject resource = new ResourceProject();
    private static final Logger log  = Logger.getLogger(ComRequisicionController.class.getName());
    
    @Autowired
    @Qualifier("daoGral")
    private GralInterfaceDao gralDao;
    
    @Autowired
    @Qualifier("daoHome")
    private HomeInterfaceDao HomeDao;
    
    //dao de procesos comerciales
    @Autowired
    @Qualifier("daoCom")
    private ComInterfaceDao ComDao;

    public ResourceProject getResource() {
        return resource;
    }
    
    public void setResource(ResourceProject resource) {
        this.resource = resource;
    }
    
    public GralInterfaceDao getGralDao() {
        return gralDao;
    }
    
    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }
    
    public ComInterfaceDao getComDao() {
        return ComDao;
    }
    
    
    @RequestMapping(value="/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response, 
            @ModelAttribute("user") UserSessionData user
            )throws ServletException, IOException {
        
        log.log(Level.INFO, "Ejecutando starUp de {0}", CompOrdenCompraController.class.getName());
        LinkedHashMap<String,String> infoConstruccionTabla = new LinkedHashMap<String,String>();
        infoConstruccionTabla.put("id", "Acciones:70");
        infoConstruccionTabla.put("folio", "Requisicion:100");
        infoConstruccionTabla.put("estado", "Estado:150");
        infoConstruccionTabla.put("momento_creacion", "fecha de alta:150");
        
        
        ModelAndView x = new ModelAndView("com_requisicion/startup", "title", "Requisici&ocute;n");
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
    
    
    
    
    
    
    @RequestMapping(value="/getAllcom_requisicion.json", method = RequestMethod.POST)
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getAllcom_requisicionJson(
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
        
        //aplicativo Orden de Compra
        Integer app_selected = 104;
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        
        //variables para el buscador
        String folio = StringHelper.isNullString(String.valueOf(has_busqueda.get("folio")));
        String fecha_inicial = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_inicial")))+"";
        String fecha_final = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_final")))+"";
        
        String data_string = app_selected+"___"+id_usuario+"___"+folio+"___"+fecha_inicial+"___"+fecha_final;
        System.out.println("Datos para el filtro del grig"+data_string);
        //obtiene total de registros en base de datos, con los parametros de busqueda
        int total_items = this.getComDao().countAll(data_string);
        
        //calcula el total de paginas
        int total_pags = resource.calculaTotalPag(total_items,items_por_pag);
        
        //variables que necesita el datagrid, para no tener que hacer uno por cada aplicativo
        DataPost dataforpos = new DataPost(orderby, desc, items_por_pag, pag_start, display_pag, input_json, cadena_busqueda,total_items,total_pags, id_user_cod);
        
        int offset = resource.__get_inicio_offset(items_por_pag, pag_start);
        
        //obtiene los registros para el grid, de acuerdo a los parametros de busqueda
       
          
        jsonretorno.put("Data", this.getComDao().getCom_requisicion_PaginaGrid(data_string, offset, items_por_pag, orderby, desc));
         //.getComOrdenCompra_PaginaGrid(data_string, offset, items_por_pag, orderby, desc));
        //obtiene el hash para los datos que necesita el datagrid
        jsonretorno.put("DataForGrid", dataforpos.formaHashForPos(dataforpos));
        
        return jsonretorno;
    }
    
    @RequestMapping(method = RequestMethod.POST, value="/getcom_requisicion.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getcom_oc_reqJson(
            @RequestParam(value="id_requisicion", required=true) String id_requisicion,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getcom_requisicionJson de {0}", CompOrdenCompraController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> datosRequisicion = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> datosGrid = new ArrayList<HashMap<String, String>>();
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        if( (id_requisicion.equals("0"))==false  ){
            datosRequisicion = this.getComDao().getCom_requisicion_Datos(Integer.parseInt(id_requisicion));
            datosGrid = this.getComDao().getCom_requisicion_DatosGrid(Integer.parseInt(id_requisicion));
        }
        
        
        jsonretorno.put("datosRequisicion", datosRequisicion);
        jsonretorno.put("datosGrid", datosGrid);
        
        return jsonretorno;
    }
    
    
    
    
    
    
   //bucador de proveedores
    //obtiene los proveedores para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/getBuscaProveedores.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getBuscaProveedoresJson(
            @RequestParam(value="rfc", required=true) String rfc,
            @RequestParam(value="no_proveedor", required=true) String no_proveedor,
            @RequestParam(value="nombre", required=true) String nombre,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        
        log.log(Level.INFO, "Ejecutando getBuscaProveedoresJson de {0}", CxpRepAntiguedadSaldosController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> proveedores = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        proveedores = this.getComDao().getBuscadorProveedores(rfc, no_proveedor, nombre,id_empresa);
        
        jsonretorno.put("Proveedores", proveedores);
        
        return jsonretorno;
    }
    

    
    
    
    //obtiene los tipos de productos para el buscador de productos
    @RequestMapping(method = RequestMethod.POST, value="/getProductoTipos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProductoTiposJson(
            @RequestParam(value="iu", required=true) String id_user_cod,
            Model model
            ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        
        ArrayList<HashMap<String, String>> arrayTiposProducto = new ArrayList<HashMap<String, String>>();
        arrayTiposProducto=this.getComDao().getProductoTipos();
        jsonretorno.put("prodTipos", arrayTiposProducto);
        
        return jsonretorno;
    }
    
    
    
    
    //Buscador de clientes
    @RequestMapping(method = RequestMethod.POST, value="/getBuscadorProductos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getBuscadorProductosJson(
            @RequestParam(value="sku", required=true) String sku,
            @RequestParam(value="tipo", required=true) String tipo,
            @RequestParam(value="descripcion", required=true) String descripcion,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        jsonretorno.put("productos", this.getComDao().getBuscadorProductos(sku,tipo,descripcion,id_empresa));
        
        return jsonretorno;
    }
    
    
    //Buscador de presentaciones de producto
    @RequestMapping(method = RequestMethod.POST, value="/getPresentacionesProducto.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getPresentacionesProductoJson(
            @RequestParam(value="sku", required=true) String sku,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        jsonretorno.put("Presentaciones", this.getComDao().getPresentacionesProducto(sku,id_empresa));
        
        return jsonretorno;
    }
    
    
    
    
    //edicion y nuevo
    @RequestMapping(method = RequestMethod.POST, value="/edit.json")
    public  @ResponseBody HashMap<String, String> editJson(
            @RequestParam(value="id_requisicion", required=true) Integer id_requisicion,
            @RequestParam(value="fecha_compromiso", required=true) String fecha_compromiso,
            @RequestParam(value="observaciones", required=true) String observaciones,
            @RequestParam(value="accion_proceso", required=true) String accion_proceso,
            @RequestParam(value="eliminado", required=false) String[] eliminado,
            @RequestParam(value="iddetalle", required=false) String[] iddetalle,
            @RequestParam(value="id_producto", required=false) String[] idproducto,
            @RequestParam(value="id_presentacion", required=false) String[] id_presentacion,
            @RequestParam(value="cantidad", required=false) String[] cantidad,
            @ModelAttribute("user") UserSessionData user
        ) {
            
            System.out.println("Guardo la Requisiscion");
            HashMap<String, String> jsonretorno = new HashMap<String, String>();
            HashMap<String, String> succes = new HashMap<String, String>();
            
            Integer app_selected = 104;
            String command_selected = "new";
            Integer id_usuario= user.getUserId();//variable para el id  del usuario
            
            String arreglo[];
            arreglo = new String[eliminado.length];
            
            for(int i=0; i<eliminado.length; i++) {
                System.out.println("Este es el id del productoo:"+idproducto[i]);
                arreglo[i]= "'"+eliminado[i] +"___" +iddetalle[i]+"___" + idproducto[i] +"___" + id_presentacion[i] +"___" + cantidad[i] +"'";
                //System.out.println(arreglo[i]);
            }
            
            //serializar el arreglo
            String extra_data_array = StringUtils.join(arreglo, ",");
            
            if( id_requisicion == 0 ){
                command_selected = "new";
            }else{
                if(accion_proceso.equals("cancelar")){
                    command_selected = accion_proceso;
                }else{
                    command_selected = "edit";
                }
            }
            
            
            String data_string = 
                    app_selected+"___"+
                    command_selected+"___"+
                    id_usuario+"___"+
                    id_requisicion+"___"+
                    fecha_compromiso+"___"+
                    observaciones.toUpperCase();
            
            
            System.out.println("data_string: "+data_string);
            
            succes = this.getComDao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
            
            log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
            String actualizo = "0";
            
            if( String.valueOf(succes.get("success")).equals("true") ){
                actualizo = this.getComDao().selectFunctionForThisApp(data_string, extra_data_array);
                jsonretorno.put("actualizo",String.valueOf(actualizo));
            }
            
            jsonretorno.put("success",String.valueOf(succes.get("success")));
            
            log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));
        return jsonretorno;
    }
}
