package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotNull;

public record AbogadoRequestDTO(
        @NotNull(message = "El ID de persona es obligatorio")
        Integer idPersona,
        @NotNull(message = "La firma es obligatorio")
        Integer idFirma,
        @NotNull(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado
) {}