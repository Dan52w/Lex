package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NivelSalarialRequestDTO(
        @NotBlank(message = "El nombre del rango salarial no debe estar Vació")
        String rango
) {
}
