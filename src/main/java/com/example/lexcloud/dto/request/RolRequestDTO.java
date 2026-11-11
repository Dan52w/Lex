package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RolRequestDTO(
        // Sin @Size, un nombre >100 (columna roles.nombre) llegaba a BD
        // y daba 500 en vez de 400 (A16).
        @Size(max = 100, message = "El nombre del rol no debe superar los 100 caracteres")
        @NotBlank(message = "El nombre del rol no puede ir vacío")
        String nombre) {
}
