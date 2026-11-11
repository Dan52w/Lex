-- ============================================================================
-- V5__super_admin.sql
-- Súper Administrador (diseño 10-nov-2026, diseno-super-admin.md).
--
-- Contenido:
--   1. Seed del rol SUPER_ADMIN (es_admin = TRUE).
--   2. Columna personas.id_firma (nullable, FK a firmas, índice).
--   3. Backfill desde abogados.id_firma donde exista.
--
-- Todo es IDEMPOTENTE (ON CONFLICT / IF NOT EXISTS / backfill solo donde
-- id_firma IS NULL / bloque DO con comprobación de pg_constraint) para que
-- la migración pueda reintentarse sin error si falla a mitad.
--
-- APLICACIÓN MANUAL: Flyway no migra en el PC de Daniel (no hay
-- flyway_schema_history pese a la configuración correcta), así que esta V5
-- se aplica a mano en pgAdmin, igual que V1–V4. NO editar V1–V4.
--
-- MIGRACIÓN DE LA CUENTA DE DANIEL (una sola vez, tras aplicar esta V5):
--   UPDATE personas SET id_rol = (SELECT id FROM roles WHERE nombre = 'SUPER_ADMIN')
--   WHERE cc = '<cédula de Daniel>';
-- Su personas.id_firma debe quedar en NULL: el súper admin es global y
-- nunca se acota a una firma. NO asignarle id_firma.
-- Los administradores de firma existentes que sean abogados quedan cubiertos
-- por el backfill; un admin NO abogado sin registro de abogado necesita:
--   UPDATE personas SET id_firma = <id de su firma> WHERE cc = '<su cédula>';
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Seed del rol SUPER_ADMIN (global; solo Daniel al inicio).
--    Re-ejecutable: si el rol ya existe, solo se asegura el flag.
-- ----------------------------------------------------------------------------
INSERT INTO roles (nombre, es_admin) VALUES ('SUPER_ADMIN', TRUE)
ON CONFLICT (nombre) DO UPDATE SET es_admin = TRUE;

-- ----------------------------------------------------------------------------
-- 2. Columna personas.id_firma (nullable, FK a firmas, índice).
--    La firma a la que pertenece la persona. Al crear un abogado se fija
--    igual a la firma del abogado; al crear un admin o miembro no abogado
--    se fija a la firma elegida. NULL = sin firma (súper admin, o persona
--    aún sin asignar). La firma SISTEMA nunca se asigna aquí: el código
--    Java la veta al crear personas/abogados/clientes.
-- ----------------------------------------------------------------------------
ALTER TABLE personas ADD COLUMN IF NOT EXISTS id_firma INTEGER;

-- La columna se crea sin FK inline a propósito: si una aplicación parcial
-- anterior ya la hubiera creado, ADD COLUMN IF NOT EXISTS la omitiría y la
-- FK quedaría sin crear. El bloque DO añade la FK solo si no existe, así
-- que cubre tanto el camino normal como el parcial.
DO $do$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'personas_id_firma_fkey'
    ) THEN
        ALTER TABLE personas
            ADD CONSTRAINT personas_id_firma_fkey
            FOREIGN KEY (id_firma) REFERENCES firmas(id);
    END IF;
END;
$do$;

CREATE INDEX IF NOT EXISTS idx_personas_id_firma ON personas(id_firma);

-- ----------------------------------------------------------------------------
-- 3. Backfill desde abogados.id_firma donde exista.
--    El WHERE id_firma IS NULL hace el backfill re-ejecutable: en una
--    segunda pasada no pisa un id_firma asignado a mano después de la
--    primera (p. ej. un admin no abogado). La persona del sistema no tiene
--    registro de abogado, así que su id_firma queda en NULL, como debe ser.
-- ----------------------------------------------------------------------------
UPDATE personas p
SET id_firma = a.id_firma
FROM abogados a
WHERE a.id_persona = p.id
  AND p.id_firma IS NULL;
