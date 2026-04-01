package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

public record PersonaResponseDTO(Integer id,
                                 String nombre,
                                 String apellido,
                                 String cc,
                                 boolean activo,
                                 String nombreRol,
                                 String telefono,
                                 LocalDateTime fechaNacimiento) {
}