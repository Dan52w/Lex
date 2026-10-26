package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PersonaRequestDTO(
        @NotBlank(message = "El nombre de la persona no puede ir vacio")
        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String nombre,
        @NotBlank(message = "El apellido de la persona no puede ir vacio")
        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String apellido,
        @NotBlank(message = "La persona debe tener una cedula para su identificación")
        @Size(min = 5, max = 20, message = "La cédula debe tener entre 5 y 20 caracteres")
        String cc,
        @NotNull(message = "La persona debe tener un rol asignado")
        Integer idRol,
        @NotBlank(message = "La persona debe tener un telefono para su comunicación")
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String telefono,
        @NotNull(message = "La persona debe tener una fecha de nacimiento")
        LocalDate fechaNacimiento
) {
}
