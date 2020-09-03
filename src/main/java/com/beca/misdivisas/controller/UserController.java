package com.beca.misdivisas.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.beca.misdivisas.interfaces.ILogRepo;
import com.beca.misdivisas.interfaces.IRolRepo;
import com.beca.misdivisas.interfaces.IUsuarioRepo;
import com.beca.misdivisas.interfaces.IUsuarioRolRepo;
import com.beca.misdivisas.jpa.Log;
import com.beca.misdivisas.jpa.Rol;
import com.beca.misdivisas.jpa.Usuario;
import com.beca.misdivisas.jpa.UsuarioRol;
import com.beca.misdivisas.model.Menu;
import com.beca.misdivisas.services.MenuService;
import com.beca.misdivisas.util.Constantes;

@Controller
public class UserController {

	@Autowired
	private IUsuarioRepo usuarioRepository;

	@Autowired
	private IRolRepo rolRepository;

	@Autowired
	private IUsuarioRolRepo usuarioRolRepository;

	@Autowired
	private ObjectFactory<HttpSession> factory;

	private BCryptPasswordEncoder encoder2;

	@Autowired
	private ILogRepo logRepo;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private MenuService menuService;
	
	@Autowired
	private IRolRepo rolRepo;

	@Autowired
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@GetMapping("/usuarioHome")
	public String userIndex(Model model) {
		model.addAttribute(Constantes.MENUES, getMenu());
		if (((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1() != null
				&& !(((Usuario) factory.getObject().getAttribute("Usuario")).getContrasena1().trim().equals(""))) {
			int idEmpresa = ((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa();
			List<Usuario> usuarios = usuarioRepository.findByIdEmpresaAndEstadoIgnoreCase(idEmpresa, "A");
			if (usuarios.size() == 0)
				usuarios = null;
			model.addAttribute(Constantes.USUARIOS, usuarios);

			registrarLog(Constantes.TEXTO_ADMINISTRAR_USUARIO, Constantes.TEXTO_ADMINISTRAR_USUARIO, Constantes.OPCION_SEGURIDAD,
					true);

			return "mainUsuarios";
		} else {
			Usuario usuario = ((Usuario) factory.getObject().getAttribute("Usuario"));
			model.addAttribute("usuario", usuario);
			return "changePassword";
		}
	}

	@GetMapping("/changePassword")
	public String changePassword(Model model) {
		model.addAttribute(Constantes.MENUES, getMenu());

		Usuario usuario = ((Usuario) factory.getObject().getAttribute("Usuario"));
		model.addAttribute("usuario", usuario);
		return "changePassword";
	}

	@GetMapping("/usuarioMainAgregar")
	public String showSignUpForm(Model model) {
		model.addAttribute(Constantes.MENUES, getMenu());

		Usuario usuario = new Usuario();
		usuario.setHabilitado(false);
		model.addAttribute("usuario", usuario);
		List<Rol> roles = rolRepo.findByIdEmpresaAndEstado(((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa(), Constantes.ACTIVO);
		Collections.sort(roles);
		model.addAttribute(Constantes.ROLES, roles);
		
		List<Rol> rolesSelect = new ArrayList<Rol>();
		
		model.addAttribute(Constantes.ROLES_SELECT, rolesSelect);
		
		return "addUsuario";
	}

	@GetMapping("usuarioListar")
	public String showUpdateForm(Model model) {
		model.addAttribute(Constantes.MENUES, getMenu());

		int idEmpresa = ((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa();
		model.addAttribute(Constantes.USUARIOS, usuarioRepository.findByIdEmpresaAndEstadoIgnoreCase(idEmpresa, "A"));

		return "mainUsuarios";
	}

	@GetMapping("resultadoCambio")
	public String showResultado(Model model) {

		return "verResultado";
	}

	@PostMapping("usuarioAgregar")
	public String addUsuario(@Valid Usuario usuario, BindingResult result, Model model) {

		if (usuarioRepository.findByNombreUsuarioIgnoreCaseAndEstadoIgnoreCase(usuario.getNombreUsuario(), "A") != null)
			result.rejectValue("nombreUsuario", "", "ya esta siendo utilizado");
		else if (!sonValidosCaracteres(usuario.getNombreUsuario()))
			result.rejectValue("nombreUsuario", "", "caracteres especiales validos @ . _ -");

		if (usuario.getContrasena().length() < 8 || usuario.getContrasena().length() > 20)
			result.rejectValue("contrasena", "", "debe contener entre 8 y 20 caracteres");
		else if (!esValidaContrasena(usuario.getContrasena()))
			result.rejectValue("contrasena", "",
					"debe contener al menos una mayúscula, una minúscula, un número y un caracter especial ! @ # $ * . _");// %
																															// &

		else if (!usuario.getContrasena().equals(usuario.getRepitaContrasena()))
			result.rejectValue("repitaContrasena", "", "debe coincidir");

		if (result.hasErrors()) {
			return "addUsuario";
		}

		// Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// String userName = auth.getName();
		// Usuario usuarioAuth = usuarioRepository.findByNombreUsuario(userName);

		Date date = new Date();
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);

		usuario.setFechaActualizacionContrasena(ts);
		int idEmpresa = ((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa();
		usuario.setIdEmpresa(idEmpresa);
		encoder2 = new BCryptPasswordEncoder();
		usuario.setContrasena(encoder2.encode(usuario.getContrasena()));
		usuario.setEstado("A");

		// model.addAttribute("usuarioRep", usuarioRepository.findAll());
		usuarioRepository.save(usuario);

		UsuarioRol usuarioRol = new UsuarioRol();

		if (((Usuario) factory.getObject().getAttribute("Usuario")).getTipoUsuario().equalsIgnoreCase("Interno"))
			usuarioRol.setIdRol(rolRepository.findByRol("ADMIN").getIdRol());
		else
			usuarioRol.setIdRol(rolRepository.findByRol("CONSULTOR").getIdRol());

		usuarioRol.setIdUsuario(usuarioRepository
				.findByNombreUsuarioIgnoreCaseAndEstadoIgnoreCase(usuario.getNombreUsuario(), "A").getIdUsuario());
		usuarioRolRepository.save(usuarioRol);

		String detalle = "Agregar Usuario : NombreUsuario(" + usuario.getNombreUsuario() + ");  idUsuario("
				+ usuario.getIdUsuario() + ");";
		registrarLog(Constantes.TEXTO_ADMINISTRAR_USUARIO, detalle, Constantes.OPERACION_CREAR, true);

		return "redirect:usuarioListar?success";
	}

	@PostMapping("usuarioEditar")
	public String showUpdateForm(@RequestParam("usuarioId") int id, Model model) {
		model.addAttribute(Constantes.MENUES, getMenu());

		int idEmpresa = ((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa();
		Usuario usuarioRep = usuarioRepository.findById(id);

		if (!usuarioRep.getIdEmpresa().equals(idEmpresa))

			return "redirect:/usuarioListar?error";

		else {
			usuarioRep.setContrasena(null);			
			List<Rol> roles = rolRepo.findByIdEmpresaAndEstado(((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa(), Constantes.ACTIVO);			
			
			List<Rol> rolesSelect = rolRepo.findByIdUsuarioAndEstado(id, Constantes.ACTIVO);
			
			if(roles != null && rolesSelect != null)
				roles.removeAll(rolesSelect);
			model.addAttribute("usuario", usuarioRep);
			model.addAttribute(Constantes.ROLES, roles);
			model.addAttribute(Constantes.ROLES_SELECT, rolesSelect);
			
			return "updateUsuario";
		}
	}

	@PostMapping("usuarioUpdate")
	public String updateUsuario(@RequestParam("usuarioId") int id, @Valid Usuario usuarioRep, BindingResult result, Model model) {
		model.addAttribute(Constantes.MENUES,getMenu());

		if (usuarioRep.getContrasena().length() < 8 || usuarioRep.getContrasena().length() > 20)
			result.rejectValue("contrasena", "", "debe contener entre 8 y 20 caracteres");
		else if (!esValidaContrasena(usuarioRep.getContrasena()))
			result.rejectValue("contrasena", "",
					"debe contener al menos una mayúscula, una minúscula, un número y un caracter especial ! @ # $ * . _");
		else if (!usuarioRep.getContrasena().equals(usuarioRep.getRepitaContrasena()))
			result.rejectValue("repitaContrasena", "", "debe coincidir");

		Usuario usuario = (Usuario) usuarioRepository.findById(id);

		usuario.setContrasena5(usuario.getContrasena4());
		usuario.setContrasena4(usuario.getContrasena3());
		usuario.setContrasena3(usuario.getContrasena2());
		usuario.setContrasena2(usuario.getContrasena1());
		usuario.setContrasena1(usuario.getContrasena());
		usuario.setContrasena(usuarioRep.getContrasena());

		usuarioRep.setNombreUsuario(usuario.getNombreUsuario());

		if (!esValidaUltimasContrasenas(usuario))
			result.rejectValue("contrasena", "", "no puede ser igual a las últimas 5 utilizadas");

		if (result.hasErrors()) {
			usuarioRep.setIdUsuario(id);
			return "updateUsuario";
		}

		// Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// String userName = auth.getName();
		// Usuario usuarioAuth = usuarioRepository.findByNombreUsuario(userName);

		Date date = new Date();
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);

		usuarioRep.setFechaActualizacionContrasena(ts);

		encoder2 = new BCryptPasswordEncoder();
		usuarioRep.setContrasena(encoder2.encode(usuarioRep.getContrasena()));

		usuario.setContrasena(usuarioRep.getContrasena());
		usuario.setNombreCompleto(usuarioRep.getNombreCompleto());
		usuario.setHabilitado(usuarioRep.getHabilitado() == null ? false : usuarioRep.getHabilitado());
		usuario.setEmail(usuarioRep.getEmail());
		usuario.setFechaActualizacionContrasena(usuarioRep.getFechaActualizacionContrasena());

		usuarioRepository.save(usuario);
		// model.addAttribute("usuarioRep", usuarioRepository.findAll());
		String detalle = "Editar Usuario : NombreUsuario(" + usuario.getNombreUsuario() + ");  idUsuario("
				+ usuario.getIdUsuario() + ");";

		registrarLog(Constantes.TEXTO_ADMINISTRAR_USUARIO, detalle, Constantes.OPERACION_EDICION, true);
		return "redirect:/usuarioListar?success";
	}

	@PostMapping("usuarioChange")
	public String cambioContrasenaUsuario(@RequestParam("usuarioId") int id, @Valid Usuario usuarioRep, BindingResult result, Model model) {
		model.addAttribute(Constantes.MENUES,getMenu());

		Usuario usuario = (Usuario) usuarioRepository.findById(id);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		if (!encoder.matches(usuarioRep.getContrasena(), usuario.getContrasena()))
			result.rejectValue("contrasena", "", "no coincide con la actual");
		else if (!usuarioRep.getNuevaContrasena().equals(usuarioRep.getRepitaContrasena()))
			result.rejectValue("repitaContrasena", "", "debe coincidir con la nueva");

		if (usuarioRep.getNuevaContrasena().length() < 8 || usuarioRep.getNuevaContrasena().length() > 20)
			result.rejectValue("nuevaContrasena", "", "debe contener entre 8 y 20 caracteres");
		else if (!esValidaContrasena(usuarioRep.getNuevaContrasena()))
			result.rejectValue("nuevaContrasena", "",
					"debe contener al menos una mayúscula, una minúscula, un número y un caracter especial ! @ # $ * . _");

		usuario.setContrasena5(usuario.getContrasena4());
		usuario.setContrasena4(usuario.getContrasena3());
		usuario.setContrasena3(usuario.getContrasena2());
		usuario.setContrasena2(usuario.getContrasena1());
		usuario.setContrasena1(usuario.getContrasena());
		usuario.setContrasena(usuarioRep.getNuevaContrasena());

		if (!esValidaUltimasContrasenas(usuario))
			result.rejectValue("nuevaContrasena", "", "no puede ser igual a las últimas 5 utilizadas");

		if (result.hasErrors()) {
			usuarioRep.setIdUsuario(id);
			return "changePassword";
		}

		// Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// String userName = auth.getName();
		// Usuario usuarioAuth = usuarioRepository.findByNombreUsuario(userName);

		Date date = new Date();
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);

		usuarioRep.setFechaActualizacionContrasena(ts);

		encoder2 = new BCryptPasswordEncoder();
		usuarioRep.setContrasena(encoder2.encode(usuarioRep.getNuevaContrasena()));

		usuario.setContrasena(usuarioRep.getContrasena());
		// usuario.setNombreCompleto(usuarioRep.getNombreCompleto());
		usuario.setHabilitado(true);
		// usuario.setEmail(usuario.getEmail());
		usuario.setFechaActualizacionContrasena(usuarioRep.getFechaActualizacionContrasena());

		usuarioRepository.save(usuario);
		factory.getObject().removeAttribute("Usuario");
		factory.getObject().setAttribute("Usuario", usuario);
		// model.addAttribute("usuarioRep", usuarioRepository.findAll());

		registrarLog(Constantes.TEXTO_ADMINISTRAR_USUARIO, Constantes.CAMBIO_CLAVE, Constantes.OPERACION_EDICION, true);

		return "redirect:/resultadoCambio?success";
	}

	@PostMapping("usuarioEliminar")
	public String deleteUsuario(@RequestParam("usuarioId") int id, Model model) {

		Usuario usuario = usuarioRepository.findById(id);

		int idEmpresa = ((Usuario) factory.getObject().getAttribute("Usuario")).getIdEmpresa();

		if (!usuario.getIdEmpresa().equals(idEmpresa))

			return "redirect:/usuarioListar?error";

		else {

			usuario.setEstado("I");
			usuarioRepository.save(usuario);
			String detalle = "Eliminar Usuario : NombreUsuario(" + usuario.getNombreUsuario() + ");  idUsuario("
					+ usuario.getIdUsuario() + ");";

			registrarLog(Constantes.TEXTO_ADMINISTRAR_USUARIO, detalle, Constantes.OPERACION_BORRAR, true);
			return "redirect:/usuarioListar?success";
		}
	}

	/*
	 * public static void main(String[] args) { //1 mayuscula, 1 minuscula, 1 numero
	 * minimo String password = "Cristian199.!@#$%&*./";
	 * System.out.println(esValidaContrasena(password)); BCryptPasswordEncoder
	 * encoder = new BCryptPasswordEncoder();
	 * System.out.println(encoder.encode("#C0nsu%t*r."));
	 * System.out.println(encoder.matches("#C0nsu%t*r.",
	 * "$2a$10$mZVkokLHduInspHBIv7Ur.rUcggdFe.eUfaTEo9laHEYKnWhCHQcq"));
	 * 
	 * }
	 */

	private static boolean esValidaUltimasContrasenas(Usuario usuario) {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		if (encoder.matches(usuario.getContrasena(), usuario.getContrasena1()))
			return false;
		if (encoder.matches(usuario.getContrasena(), usuario.getContrasena2()))
			return false;
		if (encoder.matches(usuario.getContrasena(), usuario.getContrasena3()))
			return false;
		if (encoder.matches(usuario.getContrasena(), usuario.getContrasena4()))
			return false;
		if (encoder.matches(usuario.getContrasena(), usuario.getContrasena5()))
			return false;
		else
			return true;
	}

	private static boolean esValidaContrasena(String contrasena) {

		char clave;
		byte contNumero = 0, contLetraMay = 0, contLetraMin = 0, contEspecial = 0;
		for (byte i = 0; i < contrasena.length(); i++) {
			clave = contrasena.charAt(i);
			String passValue = String.valueOf(clave);
			if (passValue.matches("[A-Z]"))
				contLetraMay++;
			else if (passValue.matches("[a-z]"))
				contLetraMin++;
			else if (passValue.matches("[0-9]"))
				contNumero++;
			else if (passValue.matches("[!@#$*._]"))
				contEspecial++;

		}

		if (contLetraMay > 0 && contLetraMin > 0 && contNumero > 0 && contEspecial > 0
				&& contrasena.length() == (contLetraMay + contLetraMin + contNumero + contEspecial))
			return true;
		else
			return false;
	}

	private static boolean sonValidosCaracteres(String texto) {

		char clave;
		byte contNumero = 0, contLetraMay = 0, contLetraMin = 0, contEspecial = 0;
		for (byte i = 0; i < texto.length(); i++) {
			clave = texto.charAt(i);
			String passValue = String.valueOf(clave);
			if (passValue.matches("[A-Z]"))
				contLetraMay++;
			else if (passValue.matches("[a-z]"))
				contLetraMin++;
			else if (passValue.matches("[0-9]"))
				contNumero++;
			else if (passValue.matches("[@._-]"))
				contEspecial++;

		}

		if (texto.length() == (contLetraMay + contLetraMin + contNumero + contEspecial))
			return true;
		else
			return false;
	}

	public void registrarLog(String accion, String detalle, String opcion, boolean resultado) {
		Date date = new Date();
		Log audit = new Log();

		String ip = request.getRemoteAddr();
		HttpSession session = factory.getObject();
		Usuario us = (Usuario) session.getAttribute("Usuario");

		audit.setFecha(new Timestamp(date.getTime()));
		audit.setIpOrigen(ip);
		audit.setAccion(accion);
		audit.setDetalle(detalle);
		audit.setIdEmpresa(us.getIdEmpresa());
		audit.setIdUsuario(us.getIdUsuario());
		audit.setNombreUsuario(us.getNombreUsuario());
		audit.setOpcionMenu(opcion);
		audit.setResultado(true);
		logRepo.save(audit);
		logger.info("Ip origen: " + ip + " Accion:" + accion + " Detalle:" + detalle + " Opcion:" + opcion);
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