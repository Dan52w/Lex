package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneroRequestDTO(
        @NotBlank(message = "El nombre del Genero no debe estar Vació")
        @Size(max = 30, message = "El campo no puede superar 30 caracteres")
        String nombre
) {
}
