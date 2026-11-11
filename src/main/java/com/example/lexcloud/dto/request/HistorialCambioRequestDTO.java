package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record HistorialCambioRequestDTO(
        // M19: la fecha la fija el servidor y se ignora la que venga aquí;
        // el campo se conserva por compatibilidad con el front y con los
        // llamadores existentes (VinculoCasoServiceImpl.auditar).
        LocalDateTime fecha,
        @NotBlank(message = "Debe haber una breve descripción del cambio o actualización")
        String descripcion,
        @NotNull(message = "Debe haber un caso referenciado")
        Integer idCaso,
        @NotNull(message = "Debe haber una persona referenciada al cambio")
        Integer idPersona) {
}
