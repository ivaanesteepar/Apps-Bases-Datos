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
		try {
		        con = pool.getConnection();
		        con.setAutoCommit(false);
		        
		     	// Imprimir las filas de la tabla tickets y las horas de salida antes de la anulación
		        String queryBefore = "SELECT t.idTicket, v.fecha, r.horaSalida, t.cantidad, t.precio " +
		                             "FROM tickets t " +
		                             "INNER JOIN viajes v ON t.idViaje = v.idViaje " +
		                             "INNER JOIN recorridos r ON v.idRecorrido = r.idRecorrido";
		        st = con.prepareStatement(queryBefore);
		        rs = st.executeQuery();
		        
		        System.out.println("Filas de la tabla tickets antes de la anulación:");
		        while (rs.next()) {
		            int idTicket = rs.getInt("idTicket");
		            Date fechaCompra = rs.getDate("fecha");
		            Time horaSalida = rs.getTime("horaSalida");
		            int cantidad = rs.getInt("cantidad");
		            double precio = rs.getDouble("precio");
	
		            System.out.println("ID Ticket: " + idTicket + ", Fecha Viaje: " + fechaCompra +
		                               ", Hora Salida: " + horaSalida + ", Cantidad: " + cantidad +
		                               ", Precio: " + precio);
		        }
		       
		        SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy");
		        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");
	
		        String fechaFormateada = sdfFecha.format(fecha);
		        String horaFormateada = sdfHora.format(hora);
	
		        System.out.println("Fecha formateada: " + fechaFormateada);
		        System.out.println("Hora formateada: " + horaFormateada);
	
		     	// Consulta SQL para verificar la existencia del viaje
		        st = con.prepareStatement("SELECT idViaje, nPlazasLibres FROM viajes JOIN recorridos ON viajes.idRecorrido = recorridos.idRecorrido "
		                + "WHERE estacionOrigen = ? AND estacionDestino = ? "
		                + "AND TO_CHAR(horaSalida, 'HH24:MI') = ? AND TO_CHAR(fecha, 'DD/MM/YYYY') = ?");
		        
		        st.setString(1, origen);
		        st.setString(2, destino);
		        st.setString(3, horaFormateada);
		        st.setString(4, fechaFormateada);
		        rs = st.executeQuery();
		        
			// Comprobar si hay al menos una fila en el ResultSet
		        if (!rs.next()) {
		            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
		        }
		        
		        // Obtener valores del ResultSet
		        int idViaje = rs.getInt("idViaje");
		        int nPlazasLibres = rs.getInt("nPlazasLibres");
		       
		        // Verificar si hay suficientes plazas libres
		        if (nPlazasLibres < nroPlazas) {
		            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
		        }
	
		        // Consulta SQL para anular el billete
		        st = con.prepareStatement("DELETE FROM tickets WHERE idTicket = ?");
		        st.setInt(1, ticket);
		        
		        int filasBorradas = st.executeUpdate();
	
		        if (filasBorradas == 0) {
		            System.out.println("No se ha anulado ningún billete.");
		        } else {
		            System.out.println("Se ha anulado el billete correctamente.");
		            con.commit();
		        }
		        
		    	// Imprimir las filas de la tabla tickets y las horas de salida después de la anulación
		        String queryAfter = "SELECT t.idTicket, v.fecha, r.horaSalida, t.cantidad, t.precio " +
		                            "FROM tickets t JOIN viajes v ON (t.idViaje = v.idViaje) " +
		                            "JOIN recorridos r ON (v.idRecorrido = r.idRecorrido)";
		        st = con.prepareStatement(queryAfter);
		        rs = st.executeQuery();
	
		        System.out.println("Filas de la tabla tickets después de la anulación:");
		        
		        while (rs.next()) {
		            int idTicket = rs.getInt("idTicket");
		            Date fechaCompra = rs.getDate("fecha");
		            Time horaSalida = rs.getTime("horaSalida");
		            int cantidad = rs.getInt("cantidad");
		            double precio = rs.getDouble("precio");
	
		            System.out.println("ID Ticket: " + idTicket + ", Fecha Viaje: " + fechaCompra +
		                               ", Hora Salida: " + horaSalida + ", Cantidad: " + cantidad +
		                               ", Precio: " + precio);
		        }
	        
	    } catch (SQLException e) {
	        if (con != null) {
	            con.rollback();
	        }
	        LOGGER.error("Error al anular el billete: ", e);
	        throw e;
	    } finally {
	        if (st != null) {
	            st.close();
	        }
	        if (con != null) {
	            con.setAutoCommit(true);
	            con.close();
	        }
	    }
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


		// FORMATEAR FECHA
		// Crear un objeto SimpleDateFormat con el formato de fecha
	        SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");

	        // Formatear la fecha a String con el formato deseado
	        String fechaString = sdf2.format(fecha);
			


		//FORMATEAR HORA
		// Conversion a horas
	        int hours = horaTimestamp.getHours();
	        int minutes = horaTimestamp.getMinutes();

	        LocalTime horaLocalTime = LocalTime.of(hours, minutes);

	        /// Define el formato deseado para la hora
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

		// Formatea la hora local
		String horaFormateada = horaLocalTime.format(formatter);



	        // Verificar si hay plazas suficientes para el viaje
	        String query = "SELECT idViaje, nPlazasLibres FROM viajes JOIN recorridos ON viajes.idRecorrido = recorridos.idRecorrido "
	                + "WHERE estacionOrigen = ? AND estacionDestino = ? AND horaSalida = ? AND fecha = ?";
	        
	        st = con.prepareStatement(query);
	        st.setString(1, origen);
	        st.setString(2, destino);
	        st.setTimestamp(3, horaTimestamp);
	        st.setString(4, fechaString);
	        rs = st.executeQuery();

		System.out.println("origen: " + origen + " \n" + "destino: " + destino + " \n" + "hora: " + horaFormateada + " \n" + "fechaFormateada: " + fechaString + "\n");
		
		if (!rs.next()) {
	            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
	        }

	        int idViaje = rs.getInt("idViaje");
	        int nPlazasLibres = rs.getInt("nPlazasLibres");

		if (nPlazasLibres < nroPlazas) {
	            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
	        }

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
