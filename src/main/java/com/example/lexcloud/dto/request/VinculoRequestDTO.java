package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VinculoRequestDTO(
        @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
        @NotBlank(message = "El nombre del vinculo no puede ser vacío")
        String nombre) {
}
