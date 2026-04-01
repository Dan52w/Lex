package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PersonaRequestDTO(
        @NotBlank(message = "El nombre de la persona no puede ir vacio")
        String nombre,
        @NotBlank(message = "El apellido de la persona no puede ir vacio")
        String apellido,
        @NotBlank(message = "La persona debe tener una cedula para su identificación")
        @Min(value = 5, message = "La cedula de la persona no puede ser menor de 5 caracteres")
        String cc,
        @NotNull(message = "La persona debe tener un rol asignado")
        Integer idRol,
        @NotBlank(message = "La persona debe tener un telefono para su comunicación")
        String telefono,
        @NotNull(message = "La persona debe tener una fecha de nacimiento")
        LocalDateTime fechaNacimiento
) {
}
