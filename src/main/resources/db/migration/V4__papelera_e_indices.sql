-- Flyway V4: papelera de documentos (P1-B7) e índices (P1-A4).
-- documentos.eliminado_en NULL = activo; con fecha = en papelera.
-- La entidad Documento ya tiene estos campos; JPA ignora columnas extra,
-- así que V4 es segura con ddl-auto=validate y con update.
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS eliminado_en TIMESTAMP;
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS eliminado_por INTEGER;
CREATE INDEX IF NOT EXISTS idx_documentos_eliminado_en ON documentos(eliminado_en);
CREATE INDEX IF NOT EXISTS idx_documentos_caso ON documentos(id_caso);
CREATE INDEX IF NOT EXISTS idx_casos_firma ON casos(id_firma);
CREATE INDEX IF NOT EXISTS idx_auditoria_firma_fecha ON auditoria_seguridad(id_firma, fecha);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_persona ON refresh_tokens(id_persona);
