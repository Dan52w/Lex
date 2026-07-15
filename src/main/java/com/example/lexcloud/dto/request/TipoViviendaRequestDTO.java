package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoViviendaRequestDTO(
        @NotBlank(message = "El nombre del tipo de vivienda no debe estar Vació")
        String nombre
) {
}
