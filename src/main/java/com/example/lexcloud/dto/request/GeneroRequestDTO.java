package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneroRequestDTO(
        @Size(max = 50, message = "El nombre no debe superar los 50 caracteres")
        @NotBlank(message = "El nombre del Genero no debe estar Vació")
        String nombre
) {
}
