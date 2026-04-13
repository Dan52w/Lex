package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(
        @NotNull(message = "El id de persona es obligatorio")
        Integer idPersona,
        @Email(message = "Correo inválido")
        @NotBlank(message = "El correo es obligatorio")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}