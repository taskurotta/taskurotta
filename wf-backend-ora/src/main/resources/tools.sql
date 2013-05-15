CREATE OR REPLACE
FUNCTION get_date_from_millis(i_millis IN NUMBER)
  RETURN DATE IS
  BEGIN
    RETURN TO_DATE('01/01/1970 00:00:00', 'DD/MM/YYYY HH24:MI:SS') + (i_millis / 1000 / 60 / 60 / 24);
  END;
/

CREATE OR REPLACE
FUNCTION get_date_millis(i_date IN DATE)
  RETURN NUMBER IS
  BEGIN
    RETURN to_number(i_date - to_date('01.01.1970', 'dd.mm.yyyy')) * (24 * 60 * 60 * 1000);
  END;
/
