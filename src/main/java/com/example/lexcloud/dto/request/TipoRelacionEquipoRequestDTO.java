package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoRelacionEquipoRequestDTO(
        @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
        @NotBlank(message = "El nombre del Tipo de relacion de equipo no debe estar Vació")
        String nombre
) {
}
