/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;

import com.agnux.cfd.v2.Base64Coder;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.PocInterfaceDao;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import com.maxima.magichelpers.GlueRunner;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"user"})
@RequestMapping("/facglobal2/")
public class FacGlobalController2 {

    private static final Logger log = Logger.getLogger(FacGlobalController2.class.getName());
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

    public PocInterfaceDao getPedidDao() {
        return PedDao;
    }

    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }

    public GralInterfaceDao getGralDao() {
        return gralDao;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/goGlobal.json")
    public @ResponseBody
    HashMap<String, String> goGlobal(
            @RequestParam(value = "uid", required = true) String id_user,
            Model model) {

        Integer uid = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer cid = this.getPedidDao().getCustomerForGlobal(uid);

        String glueDir = System.getenv("HOME") + "/tools/super_prefact";
        String params = String.format("-uid %s -cid %s -d", String.valueOf(uid), String.valueOf(cid));
        HashMap<String, String> jsonOutput = new HashMap<String, String>();

        try {
            GlueRunner glueRunner = new GlueRunner(glueDir, log);
            int r = glueRunner.go("spawns_global.py", params, false, null);
            jsonOutput.put("errorCode", String.valueOf(r));
            if (r != 0) {
                jsonOutput.put("msg", "Consulte el archivo de log de esta aplicacion para mas informacion");
            } else {
                log.log(Level.INFO, "Factura global concretada con exito!!");
            }
        } catch (IOException ex) {
            Logger.getLogger(FacGlobalController2.class.getName()).log(Level.SEVERE, null, ex);
            jsonOutput.put("errorCode", "1");
            jsonOutput.put("msg", ex.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(FacGlobalController2.class.getName()).log(Level.SEVERE, null, ex);
            jsonOutput.put("errorCode", "1");
            jsonOutput.put("msg", ex.getMessage());
        }

        return jsonOutput;
    }

    @RequestMapping(value = "/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute("user") UserSessionData user)
            throws ServletException, IOException {

        log.log(Level.INFO, "Ejecutando starUp de {0}", FacGlobalController2.class.getName());
        LinkedHashMap<String, String> infoConstruccionTabla = new LinkedHashMap<String, String>();


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

         //id de usuario codificado
        x = x.addObject("iu", Base64Coder.encodeString(userId));

        return x;
    }
}
