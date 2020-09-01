package com.beca.misdivisas.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.beca.misdivisas.interfaces.ILogRepo;
import com.beca.misdivisas.jpa.Log;
import com.beca.misdivisas.jpa.Usuario;
import com.beca.misdivisas.model.Menu;
import com.beca.misdivisas.services.MenuService;
import com.beca.misdivisas.util.Constantes;

@Controller
public class FormsController {
	
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private ObjectFactory<HttpSession> factory;
	
	@Autowired
	private ILogRepo logRepo;
	
	@Autowired
	private MenuService menuService;
	
	@GetMapping(value = "/envioEfectivo")
	public String envioRemesa(Model modelo) {

		modelo.addAttribute(Constantes.MENUES,getMenu());
		if (((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1() != null
				&& !(((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1().trim().equals(""))) {

			int id = ((Usuario) factory.getObject().getAttribute("Usuario")).getEmpresa().getRif();
			modelo.addAttribute("idEmpresa", id);
			registrarLog("envioEfectivo", "Solicitud Envio de Efectivo", "Solicitud", true);
			return "envioEfectivo";
		} else {
			Usuario usuario = ((Usuario) factory.getObject().getAttribute("Usuario"));
			modelo.addAttribute("usuario", usuario);
			return "changePassword";
		}

	}

	@GetMapping(value = "/traspasoEfectivo")
	public String traspasoEfectivo(Model modelo) {
		modelo.addAttribute(Constantes.MENUES,getMenu());
		if (((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1() != null
				&& !(((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1().trim().equals(""))) {

			int id = ((Usuario) factory.getObject().getAttribute("Usuario")).getEmpresa().getRif();
			modelo.addAttribute("idEmpresa", id);
			registrarLog("TraspasoEfectivo", "Solicitud Traspaso de Efectivo", "Solicitud", true);
			return "traspasoEfectivo";

		} else {
			Usuario usuario = ((Usuario) factory.getObject().getAttribute("Usuario"));
			modelo.addAttribute("usuario", usuario);
			return "changePassword";
		}
	}

	@GetMapping(value = "/retiroEfectivo")
	public String retiroEfectivo(Model modelo) {
		modelo.addAttribute(Constantes.MENUES,getMenu());
		if (((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1() != null
				&& !(((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1().trim().equals(""))) {

			int id = ((Usuario) factory.getObject().getAttribute("Usuario")).getEmpresa().getRif();
			modelo.addAttribute("idEmpresa", id);
			registrarLog("retiroEfectivo", "Solicitud Retiro de Efectivo", "Solicitud ", true);
			return "retiroEfectivo";

		} else {
			Usuario usuario = ((Usuario) factory.getObject().getAttribute("Usuario"));
			modelo.addAttribute("usuario", usuario);
			return "changePassword";
		}
	}
	
	public void registrarLog(String accion, String detalle, String opcion, boolean resultado) {
		Date date = new Date();
		Log audit = new Log();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		String ip = request.getRemoteAddr();
		HttpSession session = factory.getObject();
		Usuario us = (Usuario) session.getAttribute("Usuario");
		if (us != null) {
			audit.setIdEmpresa(us.getIdEmpresa());
			audit.setIdUsuario(us.getIdUsuario());
			audit.setNombreUsuario(us.getNombreUsuario());
		} else {
			audit.setNombreUsuario(auth.getName());
		}
		audit.setFecha(new Timestamp(date.getTime()));
		audit.setIpOrigen(ip);
		audit.setAccion(accion);
		audit.setDetalle(detalle);
		audit.setOpcionMenu(opcion);
		audit.setResultado(true);
		logRepo.save(audit);
	}
	
	public List<Menu> getMenu() {
		List<Menu> menu = null;

		if (request.isUserInRole(Constantes.ROL_ADMIN_BECA)) {
			menu = menuService.loadMenuByRolName(Constantes.ROL_ADMIN_BECA);

		} else {
			menu = menuService.loadMenuByUserId(((Usuario) factory.getObject().getAttribute("Usuario")).getIdUsuario());
		}

		return menu;
	}
}
