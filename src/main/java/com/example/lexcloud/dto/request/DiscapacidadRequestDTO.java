package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DiscapacidadRequestDTO(
        @NotBlank(message = "El nombre del discapacidad no debe estar Vació")
        String nombre
) {
}
