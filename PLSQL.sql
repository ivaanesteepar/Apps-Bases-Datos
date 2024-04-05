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