package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ClienteRequestDTO(
        @NotBlank(message = "El Cliente debe estar referenciado por una identificación")
        @Size(max = 50, message = "La identificación no puede superar los 50 caracteres")
        String identificacion,

        @NotBlank(message = "El cliente debe tener un nombre")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "El cliente debe tener un primer apellido")
        @Size(max = 100, message = "El primer apellido no puede superar los 100 caracteres")
        String primerApellido,

        @Size(max = 100, message = "El segundo apellido no puede superar los 100 caracteres")
        String segundoApellido,

        @NotNull(message = "El Cliente debe tener una fecha de nacimiento")
        LocalDate fechaNacimiento,

        @Size(max = 150, message = "El lugar de nacimiento no puede superar los 150 caracteres")
        String lugarNacimiento,
        @Size(max = 100, message = "La nacionalidad no puede superar los 100 caracteres")
        String nacionalidad,
        @Size(max = 150, message = "La ciudad de residencia no puede superar los 150 caracteres")
        String ciudadResidencia,
        @Size(max = 150, message = "La localidad o sector no puede superar los 150 caracteres")
        String localidadSector,
        @Size(max = 150, message = "El barrio no puede superar los 150 caracteres")
        String barrio,
        @Size(max = 250, message = "La dirección de residencia no puede superar los 250 caracteres")
        String direccionResidencia,
        @Size(max = 20, message = "El teléfono de residencia no puede superar los 20 caracteres")
        String telefonoResidencia,
        @Size(max = 250, message = "La dirección de oficina no puede superar los 250 caracteres")
        String direccionOficina,
        @Size(max = 20, message = "El teléfono de oficina no puede superar los 20 caracteres")
        String telefonoOficina,

        @NotBlank(message = "El Cliente debe tener un número para su comunicación móvil")
        @Size(max = 20, message = "El teléfono móvil no puede superar los 20 caracteres")
        String telefonoMovil,

        @NotNull(message = "El cliente debe tener un correo asignado")
        @Email(message = "Debe ser un email válido")
        @Size(max = 250, message = "El correo no puede superar los 250 caracteres")
        String correo,

        // P0-B2: idFirma OPCIONAL. Si es null, el backend lo deriva del
        // token del usuario autenticado (ver ClienteServiceImpl.guardar).
        // Sin firma resoluble la creación se rechaza con 400.
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