-- ============================================================================
-- Flyway V2: tablas de seguridad (sesión revocable y auditoría).
-- Fuente: LexCloud_P0_solo.sql, con FKs e índices que el original no tenía.
--
-- Estas dos tablas todavía NO las usa el código del commit fdfb198: son la
-- base para la fase de seguridad del rearmado (refresh tokens revocables en
-- vez de JWT en JavaScript, y auditoría de quién hizo qué). Crearlas ahora
-- no cambia el comportamiento actual: quedan inertes hasta que el código
-- las use, y así el esquema ya está listo cuando llegue esa fase.
-- ============================================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    -- Corrección: el original no tenía FK. Con CASCADE, al eliminar una
    -- persona se revocan sus sesiones en vez de dejar tokens huérfanos.
    id_persona INTEGER NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expira_en TIMESTAMP NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Búsqueda del login/logout: sesiones activas de una persona, por vencimiento.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_persona_activos
    ON refresh_tokens(id_persona, revocado, expira_en);

CREATE TABLE IF NOT EXISTS auditoria_seguridad (
    id SERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Corrección: el original no tenía FKs. SET NULL conserva el registro
    -- de auditoría aunque la persona o la firma ya no existan.
    id_persona INTEGER REFERENCES personas(id) ON DELETE SET NULL,
    id_firma INTEGER REFERENCES firmas(id) ON DELETE SET NULL,
    accion VARCHAR(60) NOT NULL,
    recurso_tipo VARCHAR(60),
    recurso_id INTEGER,
    detalle VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_auditoria_firma_fecha ON auditoria_seguridad(id_firma, fecha);
CREATE INDEX IF NOT EXISTS idx_auditoria_persona_fecha ON auditoria_seguridad(id_persona, fecha);
