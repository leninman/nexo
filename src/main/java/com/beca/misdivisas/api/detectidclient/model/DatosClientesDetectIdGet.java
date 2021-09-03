package com.beca.misdivisas.api.detectidclient.model;

public class DatosClientesDetectIdGet {
	private String sharedKey;
	private String businessDescription;
	private String email;
	private String telefono;
	
	public String getSharedKey() {
		return sharedKey;
	}
	public void setSharedKey(String sharedKey) {
		this.sharedKey = sharedKey;
	}
	public String getBusinessDescription() {
		return businessDescription;
	}
	public void setBusinessDescription(String businessDescription) {
		this.businessDescription = businessDescription;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	@Override
	public String toString() {
		return "DatosClientesDetectIdGet [sharedKey=" + sharedKey + ", businessDescription=" + businessDescription
				+ ", email=" + email + ", telefono=" + telefono + "]";
	}
}