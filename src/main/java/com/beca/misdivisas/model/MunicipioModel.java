package com.beca.misdivisas.model;

public class MunicipioModel {
	private int id;
	private String nombre;
	
	public MunicipioModel(int id, String nombre) {
		this.nombre = nombre;
		this.id = id;		
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
}
