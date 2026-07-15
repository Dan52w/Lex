package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoEmpleadoRequestDTO(
        @NotBlank(message = "El nombre del Tipo Empleado no debe estar Vació")
        String nombre
) {
}
