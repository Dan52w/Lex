package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoClienteRequestDTO(
        @NotBlank(message = "El nombre del tipo de cliente no puede ir vacio")
        String nombre) {
}
