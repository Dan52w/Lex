package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotNull(message = "El id de persona es obligatorio")
        Integer idPersona,
        @Email(message = "Correo inválido")
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El campo no puede superar 255 caracteres")
        String correo,
        // P0-B7: mínimo 10 (12 para admin, validado en AuthServiceImpl).
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 10, message = "La contraseña debe tener al menos 10 caracteres")
        String password
) {}