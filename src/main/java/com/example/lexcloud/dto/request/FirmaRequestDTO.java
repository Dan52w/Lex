package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FirmaRequestDTO(
        @NotBlank(message = "La firma debe tener un Nombre")
        String nombre,
        @NotBlank(message = "La firma debe estar asociada a un NIT")
        String nit,
        @NotNull(message = "Debe indicar el número de integrantes de la firma")
        @Min(value = 0, message = "El numero mínimo de integrantes para una firma debe ser 0")
        Integer integrante) {
}