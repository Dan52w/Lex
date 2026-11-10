-- ============================================================================
-- Flyway V3: funciones y triggers de reglas de negocio.
-- Fuente: 006, 007, 009, 010, 012 y 013 (008 no se crea: ver abajo).
-- Se crean DESPUÉS de V1 a propósito: los seeds de V1 (caso maestro,
-- persona sistema) ya existen cuando estos triggers empiezan a aplicar.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 006_root_case.sql — todo caso sin padre cuelga del caso maestro.
-- Corrección: si el maestro no existe, antes dejaba id_caso_padre en NULL
-- en silencio y ese caso quedaba marcado como raíz (los triggers de 012
-- lo trataban como si fuera el maestro: no se podía borrar ni editar).
-- Ahora falla con un error claro en vez de crear una falsa raíz.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_root_case()
RETURNS trigger AS $$
DECLARE
    maestro_id integer;
BEGIN
    IF NEW.id_caso_padre IS NULL
       AND NEW.titulo <> 'CASO MAESTRO SISTEMA' THEN
        SELECT id INTO maestro_id
        FROM casos
        WHERE titulo = 'CASO MAESTRO SISTEMA'
        LIMIT 1;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Caso maestro del sistema no existe; no se puede asignar id_caso_padre.';
        END IF;
        NEW.id_caso_padre := maestro_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_set_root_case ON casos;
CREATE TRIGGER trg_set_root_case
BEFORE INSERT ON casos
FOR EACH ROW
EXECUTE FUNCTION set_root_case();

-- ----------------------------------------------------------------------------
-- 007_delete_records_sistema.sql — la persona sistema y el caso maestro
-- no se pueden borrar físicamente.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_delete_records_sistema()
RETURNS trigger AS $$
BEGIN
    IF TG_TABLE_NAME = 'personas' AND OLD.cc = '0000000000' THEN
        RAISE EXCEPTION 'Operación denegada: No se puede eliminar el registro base del sistema.';
    END IF;

    IF TG_TABLE_NAME = 'casos' AND OLD.titulo = 'CASO MAESTRO SISTEMA' THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Maestro es vital para la jerarquía.';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_no_delete_persona_sistema ON personas;
CREATE TRIGGER trg_no_delete_persona_sistema
BEFORE DELETE ON personas
FOR EACH ROW EXECUTE FUNCTION prevent_delete_records_sistema();

DROP TRIGGER IF EXISTS trg_no_delete_caso_raiz ON casos;
CREATE TRIGGER trg_no_delete_caso_raiz
BEFORE DELETE ON casos
FOR EACH ROW EXECUTE FUNCTION prevent_delete_records_sistema();

-- ----------------------------------------------------------------------------
-- 008_subtipo_exists.sql — NO se crea.
-- La FK casos.id_subtipo_caso -> subtipo_casos(id) ya garantiza que el
-- subtipo existe: el trigger solo repetía esa comprobación con un SELECT
-- extra en cada INSERT/UPDATE. Si existe en una base vieja, se elimina.
-- ----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_validate_subtipo ON casos;
DROP FUNCTION IF EXISTS validate_subtipo_exists();

-- ----------------------------------------------------------------------------
-- 009_default_estado.sql — si un caso llega sin estado, queda SIN DEFINIR.
-- (La columna es NOT NULL; el trigger BEFORE INSERT lo rellena antes de
-- que se compruebe la restricción.)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_default_estado()
RETURNS trigger AS $$
BEGIN
    IF NEW.id_estado IS NULL THEN
        SELECT id INTO NEW.id_estado
        FROM estados
        WHERE nombre = 'SIN DEFINIR'
        LIMIT 1;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_default_estado ON casos;
CREATE TRIGGER trg_default_estado
BEFORE INSERT ON casos
FOR EACH ROW
EXECUTE FUNCTION set_default_estado();

-- ----------------------------------------------------------------------------
-- 010_soft_delete_personas.sql — borrar una persona solo la desactiva.
-- Corrección menor: se quitó el RAISE NOTICE por cada borrado (ruido en
-- el log; el rastro queda en historial/auditoría, no en avisos).
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION soft_delete_personas()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.cc = '0000000000' THEN
        RAISE EXCEPTION 'Operación denegada: El registro raíz del sistema no puede ser alterado.';
    END IF;

    UPDATE personas
    SET activo = FALSE
    WHERE id = OLD.id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_soft_delete_personas ON personas;
CREATE TRIGGER trg_soft_delete_personas
BEFORE DELETE ON personas
FOR EACH ROW
EXECUTE FUNCTION soft_delete_personas();

-- ----------------------------------------------------------------------------
-- 012_prevent_delete_root_case.sql — el caso raíz no se borra ni se edita.
-- (Es el único con id_caso_padre NULL; V1 garantiza que solo el maestro
-- está en ese estado.)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_delete_root_case()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.id_caso_padre IS NULL THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz es un registro del sistema y no puede ser eliminado.';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_delete_root_case ON casos;
CREATE TRIGGER trg_prevent_delete_root_case
BEFORE DELETE ON casos
FOR EACH ROW
EXECUTE FUNCTION prevent_delete_root_case();

CREATE OR REPLACE FUNCTION prevent_update_caso_raiz()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.id_caso_padre IS NULL THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz del sistema es de solo lectura para integridad de la jerarquía.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_update_caso_raiz ON casos;
CREATE TRIGGER trg_prevent_update_caso_raiz
BEFORE UPDATE ON casos
FOR EACH ROW
EXECUTE FUNCTION prevent_update_caso_raiz();

-- ----------------------------------------------------------------------------
-- 013_soft_delete_casos.sql — borrar un caso solo lo desactiva y deja
-- constancia en historial_cambios firmada por la persona sistema.
-- Antes fallaba siempre: la persona sistema no estaba sembrada y el
-- INSERT dejaba id_persona en NULL (violación de NOT NULL). V1 ya la
-- siembra, así que este trigger ahora funciona.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION soft_delete_casos()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.id_caso_padre IS NULL THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz del sistema debe permanecer siempre activo.';
    END IF;

    UPDATE casos
    SET activo = FALSE
    WHERE id = OLD.id;

    INSERT INTO historial_cambios (
        id_caso,
        id_persona,
        fecha,
        descripcion
    )
    VALUES (
        OLD.id,
        (SELECT id FROM personas WHERE cc = '0000000000' LIMIT 1),
        CURRENT_TIMESTAMP,
        'BORRADO LÓGICO: El caso fue marcado como inactivo.'
    );

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_soft_delete_casos ON casos;
CREATE TRIGGER trg_soft_delete_casos
BEFORE DELETE ON casos
FOR EACH ROW
EXECUTE FUNCTION soft_delete_casos();
