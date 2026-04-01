package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubtipoCasoRequestDTO(
        @NotBlank(message = "El nombre del subtipo no puede ser vacio")
        String nombre,
        @NotNull(message = "El subtipo debe ir referenciado a un tipo caso")
        Integer tipoCaso) {
}
