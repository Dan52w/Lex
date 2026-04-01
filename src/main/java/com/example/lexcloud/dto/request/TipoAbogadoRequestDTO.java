package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoAbogadoRequestDTO(
        @NotBlank(message = "El nombre de la especialización no puede ir vacio")
        String nombre) {
}
