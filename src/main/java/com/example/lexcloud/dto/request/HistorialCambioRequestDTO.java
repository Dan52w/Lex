package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record HistorialCambioRequestDTO(
        @NotNull(message = "Debe haber una fecha registrada para el cambio")
        LocalDateTime fecha,
        @NotBlank(message = "Debe haber una breve descripción del cambio o actualización")
        String descripcion,
        @NotNull(message = "Debe haber un caso referenciado")
        Integer idCaso,
        @NotNull(message = "Debe haber una persona referenciada al cambio")
        Integer idPersona) {
}
