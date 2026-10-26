package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClienteRequestDTO(
        @NotBlank(message = "El Cliente debe estar referenciado por una identificación")
        @Size(max = 50, message = "El campo no puede superar 50 caracteres")
        String identificacion,

        @NotBlank(message = "El cliente debe tener un nombre")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nombres,

        @NotBlank(message = "El cliente debe tener un primer apellido")
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String primerApellido,

        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String segundoApellido,

        @NotNull(message = "El Cliente debe tener una fecha de nacimiento")
        LocalDate fechaNacimiento,

        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String lugarNacimiento,
        @Size(max = 100, message = "El campo no puede superar 100 caracteres")
        String nacionalidad,
        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String ciudadResidencia,
        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String localidadSector,
        @Size(max = 150, message = "El campo no puede superar 150 caracteres")
        String barrio,
        @Size(max = 250, message = "El campo no puede superar 250 caracteres")
        String direccionResidencia,
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String telefonoResidencia,
        @Size(max = 250, message = "El campo no puede superar 250 caracteres")
        String direccionOficina,
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String telefonoOficina,

        @NotBlank(message = "El Cliente debe tener un número para su comunicación móvil")
        @Size(max = 20, message = "El campo no puede superar 20 caracteres")
        String telefonoMovil,

        @NotNull(message = "El cliente debe tener un correo asignado")
        @Email(message = "Debe ser un email válido")
        @Size(max = 250, message = "El campo no puede superar 250 caracteres")
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