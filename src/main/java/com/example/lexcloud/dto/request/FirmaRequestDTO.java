package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FirmaRequestDTO(
        @Size(max = 200, message = "El nombre de la firma no debe superar los 200 caracteres")
        @NotBlank(message = "La firma debe tener un Nombre")
        String nombre,
        @Size(max = 50, message = "El NIT de la firma no debe superar los 50 caracteres")
        @NotBlank(message = "La firma debe estar asociada a un NIT")
        String nit,
        @NotNull(message = "Debe indicar el número de integrantes de la firma")
        @Min(value = 0, message = "El numero mínimo de integrantes para una firma debe ser 0")
        Integer integrante) {
}