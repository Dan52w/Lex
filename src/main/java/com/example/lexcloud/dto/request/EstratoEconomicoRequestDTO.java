package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EstratoEconomicoRequestDTO(
        @NotBlank(message = "El nombre del Estrato Economico no debe estar Vació")
        String nombre
) {
}
