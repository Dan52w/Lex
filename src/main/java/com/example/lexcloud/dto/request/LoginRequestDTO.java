package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "El correo no debe ser nulo")
        @NotBlank(message = "Por favor digite un correo")
        @Email(message = "Ingrese un correo valido")
        @Size(max = 255, message = "El campo no puede superar 255 caracteres")
        String correo,
        @NotBlank(message = "Digite su contraseña")
        @Size(max = 128, message = "El campo no puede superar 128 caracteres")
        String password) {
}
