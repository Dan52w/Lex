package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record FirmaRequestDTO(
        @NotBlank(message = "La firma debe tener un Nombre")
        @Size(max = 200, message = "El campo no puede superar 200 caracteres")
        String nombre,
        @NotBlank(message = "La firma debe estar asociada a un NIT")
        @Size(max = 50, message = "El campo no puede superar 50 caracteres")
        String nit,
        @Min(value = 1, message = "El numero mínimo de integrantes para una firma debe ser 1")
        Integer integrante) {
}