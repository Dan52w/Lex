package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PersonaRequestDTO(
        @Size(max = 150, message = "El nombre de la persona no debe superar los 150 caracteres")
        @NotBlank(message = "El nombre de la persona no puede ir vacio")
        String nombre,
        @Size(max = 150, message = "El apellido de la persona no debe superar los 150 caracteres")
        @NotBlank(message = "El apellido de la persona no puede ir vacio")
        String apellido,
        // M30: @Min(5) sobre un String no medía longitud (medía valor numérico).
        // La cédula es texto: longitud 5-20 (max = length de la @Column cc de
        // Persona) y solo dígitos. El patrón admite los valores sembrados
        // (la persona sistema '0000000000' es solo dígitos) y las cédulas
        // colombianas habituales; si aparecen identificaciones extranjeras con
        // letras, el patrón deberá ampliarse antes de rechazarlas en caliente.
        @NotBlank(message = "La persona debe tener una cedula para su identificación")
        @Size(min = 5, max = 20, message = "La cedula de la persona debe tener entre 5 y 20 caracteres")
        @Pattern(regexp = "\\d+", message = "La cedula de la persona solo puede contener dígitos")
        String cc,
        @NotNull(message = "La persona debe tener un rol asignado")
        Integer idRol,
        @NotBlank(message = "La persona debe tener un telefono para su comunicación")
        @Size(max = 20, message = "El telefono de la persona no puede superar los 20 caracteres")
        String telefono,
        @NotNull(message = "La persona debe tener una fecha de nacimiento")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento
) {
}
