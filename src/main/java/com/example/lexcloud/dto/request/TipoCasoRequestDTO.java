package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoCasoRequestDTO(
        @NotBlank(message = "El nombre del tipo caso no puede ir vacío")
        String nombre) {
}
