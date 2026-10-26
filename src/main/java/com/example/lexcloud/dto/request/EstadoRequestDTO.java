package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstadoRequestDTO(
        @NotBlank(message = "El nombre del estado no debe estar Vació")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nombre) {
}
