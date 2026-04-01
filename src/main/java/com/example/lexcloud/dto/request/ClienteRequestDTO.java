package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ClienteRequestDTO(
        @NotNull(message = "El Cliente debe estar referenciado a una persona")
        Integer idPersona,
        @NotNull(message = "El Cliente debe estar asociado a una Firma")
        Integer idFirma,
        LocalDateTime fecha_Vinculacion,
        Boolean activo) {
}
