package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstratoEconomicoRequestDTO(
        @NotBlank(message = "El nombre del Estrato Economico no debe estar Vació")
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String nombre
) {
}
