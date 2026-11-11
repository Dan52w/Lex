package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record SubtipoCasoRequestDTO(
        @Size(max = 150, message = "El nombre no debe superar los 150 caracteres")
        @NotBlank(message = "El nombre del subtipo no puede ser vacio")
        String nombre,
        @NotNull(message = "El subtipo debe ir referenciado a un tipo caso")
        Integer tipoCaso) {
}
