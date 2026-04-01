package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EstadoRequestDTO(
        @NotBlank(message = "El nombre del estado no debe estar Vació")
        String nombre) {
}
