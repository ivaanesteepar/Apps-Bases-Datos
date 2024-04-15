package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	@Override
	public void anularBillete(Time hora, java.util.Date fecha, String origen, String destino, int nroPlazas, int ticket)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// A completar por el alumno
	}

	@Override
	public void comprarBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas)
			throws SQLException {
		
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// A completar por el alumno
		try {
	        con = pool.getConnection();
	        con.setAutoCommit(false);

	        // Verificar si hay plazas suficientes para el viaje
	        String query = "SELECT idViaje, nPlazasLibres FROM viajes JOIN recorridos ON viajes.idRecorrido = recorridos.idRecorrido "
	                + "WHERE estacionOrigen = ? AND estacionDestino = ? AND horaSalida = ? AND fecha = ?";
	        
	        st = con.prepareStatement(query);
	        st.setString(1, origen);
	        st.setString(2, destino);
	        st.setTimestamp(3, horaTimestamp);
	        st.setDate(4, fechaSqlDate);
	        rs = st.executeQuery();

	        if (!rs.next() || rs.getInt("nPlazasLibres") < nroPlazas) {
	            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
	        }

	        int idViaje = rs.getInt("idViaje");
	        int nPlazasLibres = rs.getInt("nPlazasLibres");

	        // Insertar fila en la tabla de tickets
	        query = "INSERT INTO tickets (idViaje, fechaCompra, cantidad, precio) VALUES (?, ?, ?, ?)";
	        st = con.prepareStatement(query);
	        st.setInt(1, idViaje);
	        st.setDate(2, fechaSqlDate);
	        st.setInt(3, nroPlazas);

	        
	        // Obtener el precio del recorrido
	        query = "SELECT precio FROM recorridos WHERE idRecorrido = ?";
	        PreparedStatement stPrecio = con.prepareStatement(query);
	        stPrecio.setInt(1, idViaje);
	        ResultSet rsPrecio = stPrecio.executeQuery();

	        if (rsPrecio.next()) {
	            double precioUnitario = rsPrecio.getDouble("precio");
	            st.setDouble(4, nroPlazas * precioUnitario);
	            st.executeUpdate();
	        } 
	        else {
	            // Lanzamos una SQLException
	            throw new SQLException("Error en la obtención del precio del recorrido");
	        }

	        // Actualizar el número de plazas libres para el viaje después de la inserción
	        nPlazasLibres -= nroPlazas;
	        query = "UPDATE viajes SET nPlazasLibres = ? WHERE idViaje = ?";
	        st = con.prepareStatement(query);
	        st.setInt(1, nPlazasLibres);
	        st.setInt(2, idViaje);
	        st.executeUpdate();

	        con.commit();
	        
	    } 
		catch (SQLException e) {
	        if (con != null) {
	            con.rollback();
	        }
	        LOGGER.error("Error al comprar el billete: ", e);
	        throw e;
	    } 
		finally {
	        if (rs != null) {
	            rs.close();
	        }
	        if (st != null) {
	            st.close();
	        }
	        if (con != null) {
	            con.setAutoCommit(true);
	            con.close();
	        }
	    }
		
		
	}

}
