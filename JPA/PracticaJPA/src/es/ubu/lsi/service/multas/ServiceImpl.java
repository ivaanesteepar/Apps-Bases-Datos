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
			if (puntosRestantes.compareTo(BigDecimal.ZERO) < 0) {
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
