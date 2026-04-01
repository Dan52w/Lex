package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "El correo no debe ser nulo")
        @NotBlank(message = "Por favor digite un correo")
        @Email(message = "Ingrese un correo valido")
        String correo,
        @NotBlank(message = "Digite su contraseña")
        String password) {
}
