package es.ubu.lsi.model.multas;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Set;


/**
 * The persistent class for the CONDUCTOR database table.
 * 
 */
@Entity
public class Conductor implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String nif;

	private String apellido;

	private DireccionPostal direccion;

	private String nombre;

	private BigDecimal puntos;

	//bi-directional many-to-one association to Vehiculo
	@ManyToOne
	@JoinColumn(name="IDAUTO")
	private Vehiculo vehiculo;

	//bi-directional many-to-one association to Incidencia
	@OneToMany(mappedBy="conductor")
	private Set<Incidencia> incidencias;

	public Conductor() {
	}

	public String getNif() {
		return this.nif;
	}

	public void setNif(String nif) {
		this.nif = nif;
	}

	public String getApellido() {
		return this.apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
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

	public BigDecimal getPuntos() {
		return this.puntos;
	}

	public void setPuntos(BigDecimal puntos) {
		this.puntos = puntos;
	}

	public Vehiculo getVehiculo() {
		return this.vehiculo;
	}

	public void setVehiculo(Vehiculo vehiculo) {
		this.vehiculo = vehiculo;
	}

	public Set<Incidencia> getIncidencias() {
		return this.incidencias;
	}

	public void setIncidencias(Set<Incidencia> incidencias) {
		this.incidencias = incidencias;
	}

		
	@Override
	public String toString() {
		return "Conductor: NIF - " + nif + ", nombre - " + nombre + ", apellido - " 
				+ apellido + ", direccion postal - " + direccion + ", vehiculo - " 
				+ vehiculo + ", puntos - " + puntos + ", incidencias - " + incidencias;
	}
}
