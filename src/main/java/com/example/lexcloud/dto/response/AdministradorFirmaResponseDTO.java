package com.example.lexcloud.dto.response;

/**
 * Respuesta de GET /api/firmas/{id}/administradores: personas con rol
 * ADMINISTRADOR de una firma. El correo viene del login de la persona y
 * puede ser null (persona sin credenciales creadas aún).
 */
public record AdministradorFirmaResponseDTO(
        Integer id,
        String nombre,
        String apellido,
        String cc,
        String correo,
        String nombreRol
) {
}
