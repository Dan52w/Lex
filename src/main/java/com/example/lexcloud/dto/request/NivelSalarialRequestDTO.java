package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NivelSalarialRequestDTO(
        @NotBlank(message = "El nombre del rango salarial no debe estar Vació")
        @Size(max = 50, message = "El campo no puede superar 50 caracteres")
        String rango
) {
}
