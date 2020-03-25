package com.beca.misdivisas.services;

import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.beca.misdivisas.interfaces.ILogRepo;
import com.beca.misdivisas.jpa.Log;
import com.beca.misdivisas.jpa.Usuario;

@Service
public class LogService {

	
	public void registrar(HttpSession sesion, HttpServletRequest request,  ILogRepo logRepo) {
		Date date = new Date();
		Log audit = new Log();
		String ip = request.getRemoteAddr();
		Usuario us = (Usuario) sesion.getAttribute("Usuario");
		if (us != null) {
			audit.setIdEmpresa(us.getIdEmpresa());
			audit.setIdUsuario(us.getIdUsuario());
			audit.setNombreUsuario(us.getNombreUsuario());
		}
		audit.setFecha(new Timestamp(date.getTime()));
		audit.setIpOrigen(ip);
		audit.setAccion("Logout");
		audit.setDetalle("Logout");
		audit.setOpcionMenu("Cerrar Sesion");
		audit.setResultado(true);
		logRepo.save(audit);
	}
}
