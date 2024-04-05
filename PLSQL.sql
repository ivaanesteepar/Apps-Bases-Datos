drop table clientes cascade constraints;
drop table abonos   cascade constraints;
drop table eventos  cascade constraints;
drop table reservas	cascade constraints;

drop sequence seq_abonos;
drop sequence seq_eventos;
drop sequence seq_reservas;


-- Creación de tablas y secuencias

create table clientes(
	NIF	varchar(9) primary key,
	nombre	varchar(20) not null,
	ape1	varchar(20) not null,
	ape2	varchar(20) not null
);


create sequence seq_abonos;

create table abonos(
	id_abono	integer primary key,
	cliente  	varchar(9) references clientes,
	saldo	    integer not null check (saldo>=0)
    );

create sequence seq_eventos;

create table eventos(
	id_evento	integer  primary key,
	nombre_evento		varchar(20),
    fecha       date not null,
	asientos_disponibles	integer  not null
);

create sequence seq_reservas;

create table reservas(
	id_reserva	integer primary key,
	cliente  	varchar(9) references clientes,
    evento      integer references eventos,
	abono       integer references abonos,
	fecha	date not null
);


	
-- Procedimiento a implementar para realizar la reserva
create or replace procedure reservar_evento( arg_NIF_cliente varchar,
 arg_nombre_evento varchar, arg_fecha date) is
 -- Declaración de excepciones
  EVENTO_PASADO exception;
  pragma exception_init(EVENTO_PASADO, -20001);
  msg_evento_pasado constant varchar(90) := 'No se pueden reservar el evento ' || arg_nombre_evento || ', pues ya ha pasado';
  

  CLIENTE_INEXISTENTE exception;
  pragma exception_init(CLIENTE_INEXISTENTE, -20002);
  msg_cliente_inexistente constant varchar(50) := 'Cliente inexistente';
  
  
  EVENTO_INEXISTENTE exception;
  pragma exception_init(EVENTO_INEXISTENTE, -20003);
  msg_evento_inexistente constant varchar(50) := 'El evento ' || arg_nombre_evento || ' no existe';
  
  
  SALDO_INSUFICIENTE exception;
  pragma exception_init(SALDO_INSUFICIENTE, -20004);
  msg_saldo_insuficiente constant varchar(50) := 'Saldo en abono insuficiente';

  
  -- Definición de variables
  vFecha eventos.fecha%type;
  vAsientos eventos.asientos_disponibles%type;
  vNIF clientes.NIF%type;
  vSaldo abonos.saldo%type;
  vIdevento eventos.id_evento%type;
    
 begin
end;
/

------ Deja aquí tus respuestas a las preguntas del enunciado:
/* P4.1: El resultado de la comprobación del paso 2 ¿sigue siendo fiable en el paso 3?

En el procedimiento 'test_reserva_evento', el paso 2 verifica si el sistema detecta correctamente un evento pasado cuando
se intenta hacer una reserva. En este caso, se reserva un evento con una fecha posterior a la fecha actual (DATE '2024-06-28'),
y se espera que el sistema genere un error indicando que el evento ya ha pasado. El manejo de esta situación se encuentra dentro
del bloque EXCEPTION correspondiente, donde se espera que se produzca un error con el código -20001, que indica que el evento ha pasado.

En el paso 3, se verifica si el sistema detecta correctamente un evento inexistente cuando se intenta hacer una reserva. Para este caso,
se intenta reservar un evento que no existe, y se espera que el sistema genere un error con el código -20003, que indica que el evento no existe.

Ambas pruebas son independientes entre sí, ya que están diseñadas para verificar diferentes aspectos del procedimiento reservar_evento.
Por lo tanto, el resultado de la comprobación en el paso 2 no afecta la fiabilidad de la comprobación en el paso 3, y viceversa. Cada paso
evalúa una condición específica y el comportamiento esperado del sistema frente a esa condición.
	
P4.2:En el paso 3, la ejecución concurrente del mismo procedimiento reservar_evento con, quizás otros o los mimos argumentos,
¿podría habernos añadido una reserva no recogida en esa SELECT que fuese incompatible con nuestra reserva?, ¿por qué?

Sí, la ejecución concurrente del mismo procedimiento reservar_evento con los mismos argumentos o incluso argumentos diferentes podría haber
añadido una reserva no recogida en la sentencia SELECT dentro del paso 3, y esta reserva podría ser incompatible con la reserva que
estamos intentando hacer. Esto se debe a que las operaciones dentro de una base de datos pueden ser concurrentes y no bloqueantes.

Dentro del procedimiento reservar_evento, primero se realiza una consulta para verificar la existencia del evento y obtener detalles
relevantes. Sin embargo, entre el momento en que se realiza esta consulta y el momento en que se intenta hacer la reserva, otra instancia
del procedimiento reservar_evento podría haber ejecutado y completado una reserva para el mismo evento, agotando los asientos disponibles.
Esto daría como resultado que nuestra consulta no recoja esta nueva reserva incompatible.

Por lo tanto, aunque nuestro procedimiento de reserva intenta verificar la existencia del evento y la disponibilidad de asientos antes de
hacer la reserva, no hay una garantía completa de que la reserva será exitosa si otras transacciones concurrentes modifican los datos relevantes
entre las consultas y las actualizaciones en nuestro procedimiento. Este tipo de situación se conoce como una "condición de carrera" y es importante
tenerla en cuenta al diseñar sistemas que manejen operaciones concurrentes en bases de datos.

P4.3

P4.4

P4.5

*/


create or replace
procedure reset_seq( p_seq_name varchar )
is
    l_val number;
begin
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by -' || l_val || 
                                                          ' minvalue 0';
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by 1 minvalue 0';

end;
/


create or replace procedure inicializa_test is
begin
  reset_seq( 'seq_abonos' );
  reset_seq( 'seq_eventos' );
  reset_seq( 'seq_reservas' );
        
  
    delete from reservas;
    delete from eventos;
    delete from abonos;
    delete from clientes;
    
       
		
    insert into clientes values ('12345678A', 'Pepe', 'Perez', 'Porras');
    insert into clientes values ('11111111B', 'Beatriz', 'Barbosa', 'Bernardez');
    
    insert into abonos values (seq_abonos.nextval, '12345678A',10);
    insert into abonos values (seq_abonos.nextval, '11111111B',0);
    
    insert into eventos values ( seq_eventos.nextval, 'concierto_la_moda', date '2024-6-27', 200);
    insert into eventos values ( seq_eventos.nextval, 'teatro_impro', date '2024-7-1', 50);

    commit;
end;
/

exec inicializa_test;

-- Completa el test

create or replace procedure test_reserva_evento is
begin
	 
  --caso 1 Reserva correcta, se realiza
  begin
    inicializa_test();
        
    DBMS_OUTPUT.PUT_LINE('T1');
        
    reservar_evento('12345678A', 'teatro_impro', DATE '2023-07-1');
        
    SELECT COUNT(*) INTO filas
    FROM reservas JOIN eventos ON (id_evento = evento)
    WHERE nombre_evento = 'teatro_impro'
    AND eventos.fecha = DATE '2023-07-1' AND reservas.cliente = '12345678A';
       
    COMMIT;
        
    --Comprobar que se ha hecho la reserva
    IF filas = 0 THEN   
        DBMS_OUTPUT.PUT_LINE('MAL: No da error pero no hace la reserva correctamente.');
    ELSE 
        DBMS_OUTPUT.PUT_LINE('BIEN: Reserva correcta.');
    END IF;  
    EXCEPTION
      WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Error en Evento: ' || SQLCODE || ' - ' || SQLERRM);
    END;
    
  end;
  
  
  --caso 2 Evento pasado
  begin
      inicializa_test();
      DBMS_OUTPUT.PUT_LINE('T2');
      reservar_evento('12345678A', 'concierto_la_moda', DATE '2024-06-28' );
    EXCEPTION
      WHEN OTHERS THEN
          IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('BIEN: Detecta evento pasado correctamente.');
          ELSE
            DBMS_OUTPUT.PUT_LINE('MAL: Da error pero no detecta evento pasado.');
            DBMS_OUTPUT.PUT_LINE('Error en Evento: '||SQLCODE);
            DBMS_OUTPUT.PUT_LINE('Mensaje '||SQLERRM);
          END IF;
    END;
  end;
  
  --caso 3 Evento inexistente
  begin
    inicializa_test;
  end;
  

  --caso 4 Cliente inexistente  
  begin
    inicializa_test;
  end;
  
  --caso 5 El cliente no tiene saldo suficiente
  begin
    inicializa_test;
  end;

  
end;
/


set serveroutput on;
exec test_reserva_evento;

select * from reservas;
