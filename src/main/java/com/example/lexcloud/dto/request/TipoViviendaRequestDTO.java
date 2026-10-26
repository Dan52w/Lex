package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoViviendaRequestDTO(
        @NotBlank(message = "El nombre del tipo de vivienda no debe estar Vació")
        @Size(max = 40, message = "El campo no puede superar 40 caracteres")
        String nombre
) {
}
