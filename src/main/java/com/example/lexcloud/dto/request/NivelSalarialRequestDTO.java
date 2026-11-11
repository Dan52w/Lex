package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NivelSalarialRequestDTO(
        @Size(max = 100, message = "El rango no debe superar los 100 caracteres")
        @NotBlank(message = "El nombre del rango salarial no debe estar Vació")
        String rango
) {
}
