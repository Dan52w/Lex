package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EstadoCivilRequestDTO(
        @NotBlank(message = "El nombre del Estado Civil no debe estar Vació")
        String nombre
) {
}
