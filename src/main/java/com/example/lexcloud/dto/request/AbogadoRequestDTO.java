package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AbogadoRequestDTO(
        @NotNull(message = "El ID de persona es obligatorio")
        Integer idPersona,
        @NotNull(message = "La firma es obligatorio")
        Integer idFirma,
        @NotNull(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado,
        @NotNull(message = "El abogado debe tener una tarjeta profesional")
        @Size(max = 255, message = "El campo no puede superar 255 caracteres")
        String tarjeta_Profesional
) {}