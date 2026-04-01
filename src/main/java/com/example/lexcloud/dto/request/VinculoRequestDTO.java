package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VinculoRequestDTO(
        @NotBlank(message = "El nombre del vinculo no puede ser vacío")
        String nombre) {
}
