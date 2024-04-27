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
		        
		        // Obtener el próximo valor de la secuencia seq_tickets
		        st = con.prepareStatement("SELECT seq_tickets.NEXTVAL FROM tickets");
		        rs = st.executeQuery();
		        int vIdTicket = 0;
		        if (rs.next()) {
		            vIdTicket = rs.getInt(1);
		            System.out.println(" idticket: " + vIdTicket);
		        }
		        
		        // Obtener el precio del recorrido
		        st = con.prepareStatement("SELECT precio FROM recorridos WHERE idRecorrido = ?");
		        st.setInt(1, idViaje);
		        ResultSet rsPrecio = st.executeQuery();
		        double precioUnitario = 0;
		        double precioTotal = 0;
		        
		        if (rsPrecio.next()) {
		            precioUnitario = rsPrecio.getDouble("precio");
		            //st.setDouble(4, nroPlazas * precioUnitario);
		            precioTotal = nroPlazas * precioUnitario;
		            //st.executeUpdate();
		            System.out.println(" precioTotal: " + precioTotal);
		        } 
		        else {
		            // Lanzamos una SQLException
		            throw new SQLException("Error en la obtención del precio del recorrido");
		        }
	
		        // Insertar fila en la tabla de tickets
		        st = con.prepareStatement("INSERT INTO tickets (idTicket, idViaje, fechaCompra, cantidad, precio) VALUES (?, ?, ?, ?, ?)");
		        
		        st.setInt(1,vIdTicket);
		        st.setInt(2, idViaje);
		        st.setDate(3, fechaSqlDate);
		        st.setInt(4, nroPlazas);
		        st.setDouble(5, precioTotal);
		        
		        int rowsInserted = st.executeUpdate();
		        if (rowsInserted > 0) {
		            System.out.println("A row has been inserted into the tickets table.");
		        } else {
		            System.out.println("No rows have been inserted into the tickets table.");
		        }
		        
		        // Actualizar el número de plazas libres para el viaje después de la inserción
		        nPlazasLibres -= nroPlazas;
		        st = con.prepareStatement("UPDATE viajes SET nPlazasLibres = ? WHERE idViaje = ?");
		        st.setInt(1, nPlazasLibres);
		        st.setInt(2, idViaje);
		        
		      
		        int filasBorradas= st.executeUpdate();
			if (filasBorradas==0) {
				System.out.println("No se ha actualizado la fila.");
			} else {
				System.out.println("Se ha actualizado la fila.");
				con.commit();
			}
				
			st = con.prepareStatement(" SELECT IDVIAJE||IDTREN||IDRECORRIDO||FECHA||NPLAZASLIBRES||REALIZADO||IDCONDUCTOR||IDTICKET||CANTIDAD||PRECIO "
					+ " FROM VIAJES natural join tickets "
					+ " where idticket=3 and trunc(fechacompra) = trunc(current_date) ");
			rs = st.executeQuery();
	
			String resultadoReal = "";
			while (rs.next()) {
				resultadoReal += rs.getString(1);
				System.out.println("resultado: " + resultadoReal);
			}
		        
		} catch (SQLException e) {
		        if (con != null) {
		            con.rollback();
		        }
		        LOGGER.error("Error al comprar el billete: ", e);
		        throw e;
		} finally {
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
