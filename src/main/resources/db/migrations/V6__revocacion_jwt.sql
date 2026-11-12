-- ============================================================================
-- V6__revocacion_jwt.sql
-- Revocación de JWT (H-03 / S2): tabla de tokens revocados por `jti`.
--
-- Contenido:
--   1. Tabla tokens_revocados (jti único, expiración, quién revocó).
--   2. Índices para la consulta del filtro y para la purga de expirados.
--
-- Todo es IDEMPOTENTE (CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT
-- EXISTS) para que la migración pueda reintentarse sin error si falla a
-- mitad, igual que V2 y V5.
--
-- APLICACIÓN MANUAL: Flyway no migra en el PC de Daniel (no hay
-- flyway_schema_history pese a la configuración correcta), así que esta V6
-- se aplica a mano en pgAdmin, igual que V1–V5. NO editar V1–V5.
--
-- NOTA DE COMPATIBILIDAD (transición): los tokens emitidos ANTES de este
-- cambio no llevan `jti` (ni `iss`/`aud`): no se pueden revocar por jti y
-- el filtro los sigue aceptando hasta que expiren (máximo 24 h, según
-- lexcloud.jwt.expiration=86400000). Pasadas 24 h del despliegue, todos
-- los tokens en circulación llevan jti y la revocación es total. Ver
-- JwtUtil (tolerancia legacy) y TokenRevocadoService.
-- ============================================================================

CREATE TABLE IF NOT EXISTS tokens_revocados (
    id SERIAL PRIMARY KEY,
    -- Identificador único del token (claim `jti`, UUID). UNIQUE: un mismo
    -- token solo puede revocarse una vez; el logout es idempotente.
    jti VARCHAR(36) NOT NULL UNIQUE,
    -- Quién revocó el token (el id de persona del principal del logout).
    -- SET NULL conserva el registro de revocación aunque la persona ya no
    -- exista; nullable porque el logout puede no resolver el principal.
    id_persona INTEGER REFERENCES personas(id) ON DELETE SET NULL,
    -- Expiración original del token (claim `exp`). La fila solo tiene
    -- sentido hasta esta fecha: pasada ella, el parser ya rechaza el token
    -- por expirado y la fila se puede purgar.
    expira_en TIMESTAMP NOT NULL,
    revocado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Consulta del filtro en cada petición autenticada: ¿está este jti revocado?
-- (La UNIQUE de jti ya crea un índice; este índice cubre la purga.)
CREATE INDEX IF NOT EXISTS idx_tokens_revocados_expira_en
    ON tokens_revocados(expira_en);
