package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EquipoAbogadoRequestDTO(
        @NotNull(message = "El ID del abogado es obligatorio.")
        Integer idAbogado,
        @NotNull(message = "El ID del miembro del personal (Persona) es obligatorio.")
        Integer idPersona,
        @NotNull(message = "El ID del tipo de relación es obligatorio.")
        Integer idTipoRelacion,
        LocalDate fechaAsignacion,
        Boolean activo) {
}
