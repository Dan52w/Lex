package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoAbogadoRequestDTO(
        @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
        @NotBlank(message = "El nombre de la especialización no puede ir vacio")
        String nombre) {
}
