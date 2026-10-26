package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoClienteRequestDTO(
        @NotBlank(message = "El nombre del tipo de cliente no puede ir vacio")
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String nombre) {
}
