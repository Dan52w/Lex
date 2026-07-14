package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClienteRequestDTO(
        @NotBlank(message = "El Cliente debe estar referenciado por una identificación")
        String identificacion,

        @NotBlank(message = "El cliente debe tener un nombre")
        String nombres,

        @NotBlank(message = "El cliente debe tener un primer apellido")
        String primerApellido,

        String segundoApellido,

        @NotNull(message = "El Cliente debe tener una fecha de nacimiento")
        LocalDate fechaNacimiento,

        String lugarNacimiento,
        String nacionalidad,
        String ciudadResidencia,
        String localidadSector,
        String barrio,
        String direccionResidencia,
        String telefonoResidencia,
        String direccionOficina,
        String telefonoOficina,

        @NotBlank(message = "El Cliente debe tener un número para su comunicación móvil")
        String telefonoMovil,

        @NotNull(message = "El cliente debe tener un correo asignado")
        @Email(message = "Debe ser un email válido")
        String correo,

        @NotNull(message = "El Cliente debe estar asociado a una Firma")
        Integer idFirma,

        @NotNull(message = "El cliente debe tener un tipo de cliente asignado (Natural/Jurídica)")
        Integer idTipoCliente,

        // IDs opcionales para la caracterización detallada (catálogos)
        Integer idTipoIdentificacion,
        Integer idGenero,
        Integer idEstadoCivil,
        Integer idEstrato,
        Integer idVivienda,
        Integer idNivelSalarial,
        Integer idTipoEmpleado,
        Integer idGradoEscolaridad,
        Integer idDiscapacidad
) {}