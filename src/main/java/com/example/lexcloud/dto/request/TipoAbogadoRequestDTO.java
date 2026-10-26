package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoAbogadoRequestDTO(
        @NotBlank(message = "El nombre de la especialización no puede ir vacio")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nombre) {
}
