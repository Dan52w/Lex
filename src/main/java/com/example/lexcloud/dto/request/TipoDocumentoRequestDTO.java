package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoDocumentoRequestDTO(
        @NotBlank(message = "El nombre del Tipo documento no puede ir vacío")
        String nombre) {
}
