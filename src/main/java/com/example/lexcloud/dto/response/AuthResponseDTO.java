package com.example.lexcloud.dto.response;

public record AuthResponseDTO(
        String token,            // <--- ¡Indispensable!
        String type,             // Ejemplo: "Bearer"
        Integer personaId,
        String nombreCompleto,
        String correo,
        String cc,
        String nombreRol,
        boolean activo
) {
    // Constructor compacto para facilitar la creación si quieres valores por defecto
    public AuthResponseDTO(String token, Integer personaId, String nombreCompleto, String correo, String cc, String nombreRol, boolean activo) {
        this(token, "Bearer", personaId, nombreCompleto, correo, cc, nombreRol, activo);
    }
}