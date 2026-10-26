-- Flyway V2: tablas nuevas del P0 (sesión revocable y auditoría).
-- Sin estas dos tablas el login falla con:
--   ERROR: relation "refresh_tokens" does not exist
-- Aplicable tanto a base nueva como a base existente (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id SERIAL PRIMARY KEY,
  id_persona INTEGER NOT NULL,
  token_hash VARCHAR(128) NOT NULL UNIQUE,
  expira_en TIMESTAMP NOT NULL,
  revocado BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS auditoria_seguridad (
  id SERIAL PRIMARY KEY,
  fecha TIMESTAMP NOT NULL,
  id_persona INTEGER,
  id_firma INTEGER,
  accion VARCHAR(60) NOT NULL,
  recurso_tipo VARCHAR(60),
  recurso_id INTEGER,
  detalle VARCHAR(500)
);
