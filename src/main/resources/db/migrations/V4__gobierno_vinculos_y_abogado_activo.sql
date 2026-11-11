-- ============================================================================
-- V4__gobierno_vinculos_y_abogado_activo.sql
-- Lote 1 de la corrección completa (revisión 2026-11-10).
--
-- Contenido:
--   1. Secuencia radicado_seq para radicados internos (C5).
--   2. Trigger un-solo-Principal por caso sobre vinculo_casos (A8).
--   3. Columna abogados.activo (A5).
--   4. Columna vinculo_casos.estado + backfill desde autorizado (M13).
--   5. Flag roles.es_admin (C3).
--
-- Todo es idempotente (IF NOT EXISTS / CREATE OR REPLACE /
-- DROP IF EXISTS + CREATE / bloque DO con EXCEPTION WHEN duplicate_object)
-- para que la migración pueda reintentarse sin error si falla a mitad.
-- NO modifica V1, V2 ni V3 (migraciones ya aplicadas; tocarlas rompería
-- los checksums de Flyway). NO elimina la columna vinculo_casos.autorizado:
-- queda como dato histórico; el código Java deja de usarla en otro lote.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Secuencia para radicados internos (C5)
-- ----------------------------------------------------------------------------
-- Los radicados internos existentes derivan del id del caso (números
-- pequeños con ceros a la izquierda, p. ej. el caso maestro usa
-- radicado_interno en ceros, ver V1). Empezar la secuencia en 1000000000
-- (1e9, 10 dígitos) evita colisiones con todos esos radicados históricos:
-- ningún id de caso llegará a 1e9 y el formato de 10 dígitos se mantiene.
CREATE SEQUENCE IF NOT EXISTS radicado_seq START WITH 1000000000 INCREMENT BY 1;

-- ----------------------------------------------------------------------------
-- 2. Un solo vínculo 'Principal' por caso (A8)
-- ----------------------------------------------------------------------------
-- Un índice parcial con subquery NO es válido en PostgreSQL, por eso la
-- regla se impone con trigger BEFORE INSERT OR UPDATE, no con índice.
-- La función cuenta TODOS los vínculos Principal del caso (autorizados
-- o no): un segundo Principal pendiente también violaría la regla.
CREATE OR REPLACE FUNCTION trg_un_solo_principal_por_caso()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $func$
DECLARE
    v_principal_id INTEGER;
    v_count        INTEGER;
BEGIN
    SELECT id INTO v_principal_id FROM vinculos WHERE nombre = 'Principal';

    -- Si el vínculo 'Principal' no existe en el catálogo, no hay nada
    -- que restringir.
    IF v_principal_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- Solo interesa cuando la fila que se inserta/actualiza es Principal.
    IF NEW.id_vinculo <> v_principal_id THEN
        RETURN NEW;
    END IF;

    -- Cuenta los Principal ya existentes del mismo caso, excluyendo la
    -- propia fila (en INSERT NEW.id aún es NULL, así que
    -- IS DISTINCT FROM NULL deja pasar todas las filas existentes;
    -- en UPDATE excluye la fila que se está modificando).
    SELECT COUNT(*) INTO v_count
    FROM vinculo_casos
    WHERE id_caso = NEW.id_caso
      AND id_vinculo = v_principal_id
      AND id IS DISTINCT FROM NEW.id;

    IF v_count > 0 THEN
        RAISE EXCEPTION 'Ya existe un vínculo Principal para el caso %', NEW.id_caso;
    END IF;

    RETURN NEW;
END;
$func$;

DROP TRIGGER IF EXISTS trg_un_solo_principal ON vinculo_casos;
CREATE TRIGGER trg_un_solo_principal
    BEFORE INSERT OR UPDATE ON vinculo_casos
    FOR EACH ROW EXECUTE FUNCTION trg_un_solo_principal_por_caso();

-- ----------------------------------------------------------------------------
-- 3. Columna activo en abogados (A5)
-- ----------------------------------------------------------------------------
-- La desactivación de un abogado se registra aquí, en su propia fila de
-- abogados, y ya no toca Persona.activo (desactivar a la persona
-- desactivaba también su login y cualquier otro rol que tuviera).
ALTER TABLE abogados ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

-- ----------------------------------------------------------------------------
-- 4. Columna estado en vinculo_casos (M13)
-- ----------------------------------------------------------------------------
-- 'estado' sustituye a 'autorizado' como fuente de verdad del gobierno del
-- vínculo. La columna 'autorizado' NO se elimina: queda como dato
-- histórico y el código Java deja de usarla en otro lote.
ALTER TABLE vinculo_casos ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';

-- Backfill desde el dato histórico: autorizado = TRUE -> AUTORIZADO,
-- autorizado = FALSE -> PENDIENTE. El WHERE estado = 'PENDIENTE' hace el
-- backfill re-ejecutable: en una segunda pasada no pisa un RECHAZADO (u
-- otro estado) decidido después de la primera.
UPDATE vinculo_casos
SET estado = CASE WHEN autorizado THEN 'AUTORIZADO' ELSE 'PENDIENTE' END
WHERE estado = 'PENDIENTE';

-- Restricción CHECK idempotente: si la constraint ya existe, el bloque
-- captura duplicate_object y no falla.
DO $do$
BEGIN
    ALTER TABLE vinculo_casos
        ADD CONSTRAINT chk_vinculo_casos_estado
        CHECK (estado IN ('PENDIENTE', 'AUTORIZADO', 'RECHAZADO'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;
$do$;

-- ----------------------------------------------------------------------------
-- 5. Flag es_admin en roles (C3)
-- ----------------------------------------------------------------------------
-- El privilegio de administrador ya no depende del nombre del rol
-- ('ADMINISTRADOR' como texto): a partir de ahora lo decide este flag.
-- El UPDATE marca el rol existente con ese nombre, si lo hay.
ALTER TABLE roles ADD COLUMN IF NOT EXISTS es_admin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE roles SET es_admin = TRUE WHERE UPPER(TRIM(nombre)) = 'ADMINISTRADOR';

-- Bootstrap del rol administrador (C3): V1 solo siembra el rol 'SISTEMA';
-- sin este seed no existiría ningún administrador y, con las escrituras de
-- roles restringidas a administradores, nadie podría crear uno. El INSERT es
-- re-ejecutable (ON CONFLICT actualiza el flag si el rol ya existe).
INSERT INTO roles (nombre, es_admin) VALUES ('ADMINISTRADOR', TRUE)
ON CONFLICT (nombre) DO UPDATE SET es_admin = TRUE;
-- NOTA DE BOOTSTRAP: el rol existe tras V4, pero hay que asignarlo a al menos
-- una persona (UPDATE personas SET id_rol = (SELECT id FROM roles WHERE
-- nombre = 'ADMINISTRADOR') WHERE cc = '<cédula del admin>'); sin ese paso
-- manual no hay ningún usuario administrador en el sistema.
