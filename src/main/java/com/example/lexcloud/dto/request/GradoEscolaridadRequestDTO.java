package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GradoEscolaridadRequestDTO(
        @NotBlank(message = "El nombre del Grado de Escolaridad no debe estar Vació")
        String nombre
) {
}
