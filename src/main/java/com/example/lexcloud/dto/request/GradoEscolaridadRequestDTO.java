package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GradoEscolaridadRequestDTO(
        @NotBlank(message = "El nombre del Grado de Escolaridad no debe estar Vació")
        @Size(max = 60, message = "El campo no puede superar 60 caracteres")
        String nombre
) {
}
