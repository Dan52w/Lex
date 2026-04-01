package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AbogadoRequestDTO(
        @NotBlank(message = "El ID de persona es obligatorio")
        Integer idPersona,
        @NotBlank(message = "La firma es obligatorio")
        Integer idFirma,
        @NotBlank(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado
) {}