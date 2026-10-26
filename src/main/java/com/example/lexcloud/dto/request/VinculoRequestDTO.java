package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VinculoRequestDTO(
        @NotBlank(message = "El nombre del vinculo no puede ser vacío")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nombre) {
}
