package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record EquipoAbogadoResponseDTO(
        Integer id,
        Integer idAbogado,
        String nombreAbogado,
        Integer idPersona,
        String nombrePersona,
        Integer idTipoRelacion,
        String nombreTipoRelacion,
        LocalDate fechaAsignacion,
        Boolean activo
) {
}
