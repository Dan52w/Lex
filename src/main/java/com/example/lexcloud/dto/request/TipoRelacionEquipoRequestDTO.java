package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoRelacionEquipoRequestDTO(
        @NotBlank(message = "El nombre del Tipo de relacion de equipo no debe estar Vació")
        String nombre
) {
}
