package com.example.lexcloud.dto.response;

public record AuthResponseDTO(Integer personaId,
                              String nombreCompleto,
                              String correo,
                              String cc,
                              String nombreRol,
                              boolean activo) {
}
