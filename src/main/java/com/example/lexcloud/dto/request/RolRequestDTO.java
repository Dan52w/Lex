package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RolRequestDTO(
        @NotBlank(message = "El nombre del rol no puede ir vacío")
        String nombre) {
}
