package es.ubu.lsi.model.multas;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Set;


/**
 * The persistent class for the VEHICULO database table.
 * 
 */
@Entity
public class Vehiculo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String idauto;

	@Embedded
	private DireccionPostal direccion;

	private String nombre;

	//bi-directional many-to-one association to Conductor
	@OneToMany(mappedBy="vehiculo")
	private Set<Conductor> conductores;

	public Vehiculo() {
	}

	public String getIdauto() {
		return this.idauto;
	}

	public void setIdauto(String idauto) {
		this.idauto = idauto;
	}

	public DireccionPostal getDireccion() {
		return this.direccion;
	}

	public void setDireccion(DireccionPostal direccion) {
		this.direccion = direccion;
	}

	public String getNombre() {
		return this.nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Set<Conductor> getConductores() {
		return this.conductores;
	}

	public void setConductores(Set<Conductor> conductores) {
		this.conductores = conductores;
	}

	@Override
	public String toString() {
		return "Vehiculo: idAuto - " + idauto + ", nombre - " 
				+ nombre + ", direccion postal - " + direccion;
	}
}
