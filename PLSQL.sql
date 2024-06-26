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
 
  begin
	 
    select eventos.fecha, eventos.asientos_disponibles into vFecha, vAsientos
    from eventos where eventos.nombre_evento = arg_nombre_evento;

    select NIF, saldo into vNIF, vSaldo
    from clientes join abonos on (NIF = cliente)
    where NIF = arg_NIF_cliente;


    -- Comprobamos que el evento y cliente existen
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        ROLLBACK;
          IF vFecha IS NULL THEN
            RAISE_APPLICATION_ERROR(-20003, msg_evento_inexistente);
          END IF;
            
          IF vNIF is NULL then
            RAISE_APPLICATION_ERROR(-20002, msg_cliente_inexistente);
          END IF;
            
      WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
    END;

    -- Comprobamos que el cliente tiene saldo
    if vSaldo <= 0 then
      raise_application_error(-20004, msg_saldo_insuficiente);
    end if;

    -- Comprobamos que el evento no ha pasado    
    IF trunc(vFecha) < trunc(arg_fecha) THEN
      RAISE_APPLICATION_ERROR(-20001, msg_evento_pasado);
    end if;
        
    -- Hacemos la reserva:
     -- Consulta para obtener el id del evento y poder realizar la reserva
    select id_evento into vIdevento
    from eventos
    where nombre_evento = arg_nombre_evento;

     -- Realización de la reserva (inserción de los argumentos en la tabla reservas)
    insert into reservas (id_reserva, cliente, evento, fecha) VALUES (seq_reservas.nextval, arg_NIF_cliente, vIdevento, arg_fecha); 

    -- Actualizamos el estado
    update abonos set saldo = saldo - 1 where cliente = arg_NIF_cliente;
    update eventos set asientos_disponibles = asientos_disponibles - 1 where nombre_evento = arg_nombre_evento;

     -- Si se ha hecho la reserva, comprobamos que se han guardado los cambios
    if sql%rowcount = 1 then 
      COMMIT;
    else
      ROLLBACK;
    end if;

end;
/

------ Deja aquí tus respuestas a las preguntas del enunciado:
/* P4.1: El resultado de la comprobación del paso 2 ¿sigue siendo fiable en el paso 3?

No, el resultado de la comprobación del paso 2 ya no es fiable en el paso 3. Esto se debe a que entre la ejecución de la comprobación 
y la operación real de reserva, pueden ocurrir cambios en los datos que invaliden las comprobaciones previas. Por ejemplo, otro proceso 
podría reservar el último asiento disponible justo antes de que se realice la reserva actual.
	

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


P4.3: ¿Qué estrategia de programación has utilizado?

Utiliza una estrategia defensiva para realizar operaciones en la base de datos de manera segura. Esta estrategia implica utilizar consultas SELECT 
para verificar si se cumplen todas las condiciones necesarias antes de realizar una transacción. Si todas las condiciones se cumplen, se llevan 
a cabo las operaciones de actualización seguidas de un commit. Sin embargo, si alguna condición no se cumple, se realiza un rollback y se lanza 
una excepción detallando el problema específico.


P4.4: ¿Cómo puede verse este hecho en tu código?

-Implementando un manejo robusto de excepciones. Se declaran excepciones personalizadas como EVENTO_PASADO, CLIENTE_INEXISTENTE, EVENTO_INEXISTENTE, 
y SALDO_INSUFICIENTE para manejar situaciones específicas.

-Realizando pruebas para asegurar el comportamiento esperado del procedimiento reservar_evento. Se implementa el procedimiento test_reserva_evento, 
que realiza pruebas automatizadas para diferentes casos, como reservas exitosas, eventos pasados, eventos inexistentes... Este procedimiento ejecuta 
el procedimiento reservar_evento con diferentes parámetros y verifica si el comportamiento es el esperado utilizando DBMS_OUTPUT.PUT_LINE para mostrar 
mensajes de estado.

-Usando transacciones controladas. Las operaciones de reserva se realizan dentro de transacciones controladas con COMMIT y ROLLBACK, 
garantizando la integridad de los datos y la reversibilidad de las acciones en caso de error.


P4.5: ¿De qué otro modo crees que podrías resolver el problema propuesto? Incluye el pseudocódigo.

Otra forma de resolver el problema podría ser implementar un mecanismo de bloqueo para evitar que varias transacciones intenten reservar el mismo evento simultáneamente. 
Esto se podría lograr utilizando bloqueos de fila o de tabla durante la operación de reserva. Esto ayuda a evitar problemas de concurrencia al garantizar que las 
operaciones de comprobación y actualización se realicen de manera segura y consistente.
El pseudocódigo para esta alternativa podría ser:

-- Inicio de la transacción
begin
  -- Bloquear la fila correspondiente al evento para evitar que otras transacciones la modifiquen
  select id_evento, asientos_disponibles into vIdevento, vAsientosDisp
  from evento
  where nombre_evento = arg_nombre_evento and fecha = arg_fecha
  for update; --bloqueo explícito
  -- Realizar las comprobaciones y operaciones de reserva como en el procedimiento original
  -- Confirmar la transacción y liberar los bloqueos
  commit;
exception
  -- Manejo de excepciones como en el procedimiento original
end;

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


create or replace procedure test_reserva_evento is

  filas INTEGER;
  vIdevento1 eventos.id_evento%TYPE; 
  vIdevento2 eventos.id_evento%TYPE;
  vSaldoAnterior abonos.saldo%TYPE;
  vSaldoActual abonos.saldo%TYPE;

begin
	 
  --caso 1 Reserva correcta, se realiza
  begin
    inicializa_test();  
    DBMS_OUTPUT.PUT_LINE('T1'); 

    -- Guardar el saldo antes de hacer la reserva
    SELECT saldo INTO vSaldoAnterior FROM abonos WHERE cliente = '12345678A';
    
    reservar_evento('12345678A', 'teatro_impro', DATE '2024-07-1');
    
    -- Se cuenta el número de filas que hay en la tabla reservas
    SELECT COUNT(*) INTO filas
    FROM reservas JOIN eventos ON (id_evento = evento)
    WHERE nombre_evento = 'teatro_impro'
    AND eventos.fecha = DATE '2024-07-1' AND reservas.cliente = '12345678A';
    
    -- Verificar la disminución del saldo
    SELECT saldo INTO vSaldoActual FROM abonos WHERE cliente = '12345678A';
 
    COMMIT;
        
    -- Comprobar que se ha hecho la reserva
    IF filas = 0 THEN   
      DBMS_OUTPUT.PUT_LINE('MAL: No da error pero no hace la reserva correctamente.');
    ELSE 
      IF vSaldoActual = vSaldoAnterior - 1 THEN
        DBMS_OUTPUT.PUT_LINE('BIEN: Reserva correcta y saldo disminuido en 1.');
      ELSE
        DBMS_OUTPUT.PUT_LINE('MAL: El saldo no ha disminuido en 1 después de la reserva.');
      END IF;
    END IF;  
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      DBMS_OUTPUT.PUT_LINE('Error en Evento: ' || SQLCODE || ' - ' || SQLERRM);
  END;
  
  
  --caso 2 Evento pasado
  begin
    inicializa_test();
    DBMS_OUTPUT.PUT_LINE('T2');
    reservar_evento('12345678A', 'concierto_la_moda', DATE '2024-06-28' ); -- Cambiado fecha futura
 
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

  
  --caso 3 Evento inexistente
  begin
    inicializa_test();
    DBMS_OUTPUT.PUT_LINE('T3');
    reservar_evento('12345678A', 'evento_inexistente', DATE '2024-06-27' ); -- Cambiado ID de evento inexistente
    
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -20003 THEN
        DBMS_OUTPUT.PUT_LINE('BIEN: Detecta evento inexistente correctamente.');
      ELSE
        DBMS_OUTPUT.PUT_LINE('MAL: Da error pero no detecta evento inexistente.');
        DBMS_OUTPUT.PUT_LINE('Error en Evento: '||SQLCODE);
        DBMS_OUTPUT.PUT_LINE('Mensaje '||SQLERRM);
      END IF;
  end;
  

  --caso 4 Cliente inexistente  
  begin
    inicializa_test();
    DBMS_OUTPUT.PUT_LINE('T4');
    reservar_evento('12345678X', 'concierto_la_moda', DATE '2024-06-27' ); -- Cambiado NIF inexistente

  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -20002 THEN
        DBMS_OUTPUT.PUT_LINE('BIEN: Detecta cliente inexistente correctamente.');
      ELSE
        DBMS_OUTPUT.PUT_LINE('MAL: Da error pero no detecta cliente inexistente.');
        DBMS_OUTPUT.PUT_LINE('Error en Evento: '||SQLCODE);
        DBMS_OUTPUT.PUT_LINE('Mensaje '||SQLERRM);
      END IF;
  end;
  

  --caso 5 El cliente no tiene saldo suficiente
  BEGIN
    inicializa_test();
    DBMS_OUTPUT.PUT_LINE('T5');
    reservar_evento('11111111B', 'concierto_la_moda', DATE '2024-06-27' ); -- NIF del cliente sin saldo

  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -20004 THEN
        DBMS_OUTPUT.PUT_LINE('BIEN: Detecta saldo insuficiente correctamente.');
      ELSE
        DBMS_OUTPUT.PUT_LINE('MAL: Da error pero no detecta saldo insuficiente.');
        DBMS_OUTPUT.PUT_LINE('Error en Evento: '||SQLCODE);
        DBMS_OUTPUT.PUT_LINE('Mensaje '||SQLERRM);
      END IF;
  END;
END;
/


set serveroutput on;
exec test_reserva_evento;

select * from reservas;
