package es.ubu.lsi.service.multas;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.dao.multas.ConductorDAO;
import es.ubu.lsi.dao.multas.TipoincidenciaDAO;
import es.ubu.lsi.model.multas.Conductor;
import es.ubu.lsi.model.multas.Incidencia;
import es.ubu.lsi.model.multas.TipoIncidencia;
import es.ubu.lsi.model.multas.Vehiculo;
import es.ubu.lsi.service.PersistenceException;
import es.ubu.lsi.service.PersistenceService;

public class ServiceImpl extends PersistenceService implements Service {
	
	private static Logger logger = LoggerFactory.getLogger(ServiceImpl.class);

	@Override
	public void insertarIncidencia(Date fecha, String nif, long tipo) throws PersistenceException {
		EntityManager em = this.createSession();
		try {
			beginTransaction(em);
			
			ConductorDAO conductorDAO = new ConductorDAO(em);
			TipoincidenciaDAO tIncidenciaDAO = new TipoincidenciaDAO(em);
			
			Conductor conductor = conductorDAO.findById(nif);
			TipoIncidencia tIncidencia = tIncidenciaDAO.findById(tipo);
			
			if (conductor == null) {
				throw new IncidentException(IncidentError.NOT_EXIST_DRIVER);
			} 
			if (tIncidencia == null) {
				throw new IncidentException(IncidentError.NOT_EXIST_INCIDENT_TYPE);
			}
			
			BigDecimal puntosConductor = conductor.getPuntos();
			BigDecimal puntosTipoIncidencia = tIncidencia.getValor();
			BigDecimal puntosRestantes = puntosConductor.subtract(puntosTipoIncidencia);
			if (puntosRestantes.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IncidentException(IncidentError.NOT_AVAILABLE_POINTS);
			}
			
			conductor.setPuntos(puntosRestantes);
			
			// Construir la consulta de inserción manualmente	        
		 Query query = em.createNativeQuery("INSERT INTO Incidencia (fecha, nif, idtipo) VALUES (:fecha, :nif, :tipo)");
		               
		 query.setParameter("fecha", fecha);
		 query.setParameter("nif", nif);
		 query.setParameter("tipo", tipo);
		        
		 // Ejecutar la consulta de inserción
		 query.executeUpdate();
		        
			commitTransaction(em);
			
		} catch (Exception e) {
			rollbackTransaction(em);
			if (e instanceof IncidentException) {
				throw e;
			}
			logger.error(e.getLocalizedMessage());
		} finally {
			em.close();
		}
	}

	@Override
	public void indultar(String nif) throws PersistenceException {
		EntityManager em = this.createSession();
		try {
			beginTransaction(em);
			
			ConductorDAO conductorDAO = new ConductorDAO(em);
			Conductor conductor = conductorDAO.findById(nif);
			
			if (conductor == null) {
				throw new IncidentException(IncidentError.NOT_EXIST_DRIVER);
			} 
			
			conductor.setPuntos(new BigDecimal("12"));
			
			// Elimina todas las incidencias del conductor utilizando una consulta de eliminación
		        Query query = em.createQuery("DELETE FROM Incidencia WHERE conductor = :conductor");
		        query.setParameter("conductor", conductor);
		        query.executeUpdate();
	
		        commitTransaction(em);

		} catch (Exception e) {
			rollbackTransaction(em);
			if (e instanceof IncidentException) {
				throw e;
			}
			logger.error(e.getLocalizedMessage());
		} finally {
			em.close();
		}
	}

	@Override
	public List<Vehiculo> consultarVehiculos() throws PersistenceException {
	    EntityManager em = this.createSession();
	    try {
	    	List<Vehiculo> vehiculos = em.createQuery("SELECT v FROM Vehiculo v JOIN FETCH v.conductores c JOIN FETCH c.incidencias", Vehiculo.class)
	                                    .getResultList();
	        for (Vehiculo vehiculo : vehiculos) {
	            System.out.println(vehiculo.toString());
	            for (Conductor conductor : vehiculo.getConductores()) {
	                System.out.println("\t" + conductor.toString());
	                for (Incidencia incidencia : conductor.getIncidencias()) {
	                    System.out.println("\t\t" + incidencia.toString());
	                }
	            }
	        }
	        return vehiculos;
	    } catch (Exception e) {
	        logger.error("Error al consultar los vehículos", e);
	        throw new PersistenceException("Error al consultar los vehículos: " + e.getMessage(), e);
	    } finally {
	        em.close();
	    }
	}
}
