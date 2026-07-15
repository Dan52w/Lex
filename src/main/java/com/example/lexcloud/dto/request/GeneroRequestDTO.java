package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GeneroRequestDTO(
        @NotBlank(message = "El nombre del Genero no debe estar Vació")
        String nombre
) {
}
