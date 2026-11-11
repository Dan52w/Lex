package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoClienteRequestDTO(
        @Size(max = 20, message = "El nombre no debe superar los 20 caracteres")
        @NotBlank(message = "El nombre del tipo de cliente no puede ir vacio")
        String nombre) {
}
