package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record PersonaResponseDTO(Integer id,
                                 String nombre,
                                 String apellido,
                                 String cc,
                                 boolean activo,
                                 String nombreRol,
                                 String telefono,
                                 // B6: la entidad Persona guarda LocalDate (fecha, sin hora);
                                 // antes se exponía como LocalDateTime. El mapeo en
                                 // PersonaMapper es ahora directo y no exige ajuste.
                                 LocalDate fechaNacimiento) {
}
