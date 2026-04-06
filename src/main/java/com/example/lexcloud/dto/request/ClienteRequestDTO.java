package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteRequestDTO(
        @NotBlank(message = "El Cliente debe estar referenciado una cedula")
        String cc,
        @NotBlank(message = "El cliente debe tener un nombre")
        String nombre,
        @NotBlank(message = "El cliente debe tener un apellido")
        String apellido,
        @NotNull(message = "El Cliente debe tener una fecha de nacimiento")
        LocalDate fechaNacimiento,
        @NotBlank(message = "El Cliente debe tener un numero para su comunicación")
        String telefono,
        @NotNull(message = "El Cliente debe estar asociado a una Firma")
        Integer idFirma,
        LocalDate fechaVinculacion) {
}
