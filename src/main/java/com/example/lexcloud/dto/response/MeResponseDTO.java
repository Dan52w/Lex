package com.example.lexcloud.dto.response;

/**
 * Respuesta de GET /api/auth/me: quién es el usuario autenticado, solo
 * para mostrar en la UI (nombre/rol) y como puente de ids. NO lleva token:
 * el llamador ya tiene el JWT. Los nombres de campo coinciden con lo que
 * el front lee en src/lib/session.ts (normalizeMe): nombreCompleto,
 * nombreRol, personaId, idFirma, correo.
 */
public record MeResponseDTO(
        Integer personaId,
        String nombreCompleto,
        String correo,
        String cc,
        String nombreRol,
        boolean activo,
        Integer idFirma
) {
}
