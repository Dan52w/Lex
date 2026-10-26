package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoCasoRequestDTO(
        @NotBlank(message = "El nombre del tipo caso no puede ir vacío")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nombre) {
}
