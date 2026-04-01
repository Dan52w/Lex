package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FirmaRequestDTO(
        @NotBlank(message = "La firma debe tener un Nombre")
        String nombre,
        @NotBlank(message = "La firma debe estar asociada a un NIT")
        String nit,
        @Min(value = 1, message = "El numero mínimo de integrantes para una firma debe ser 1")
        Integer integrante) {
}