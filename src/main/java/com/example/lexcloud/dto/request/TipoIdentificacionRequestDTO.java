package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoIdentificacionRequestDTO(
        @NotBlank(message = "El nombre del Tipo Identificacion no debe estar Vació")
        String nombre
) {
}
