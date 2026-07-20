package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record ClienteResponseDTO(
        Integer id,
        String identificacion,
        Integer idTipoIdentificacion,
        String tipoIdentificacion, // ej: "Cédula de Ciudadanía"
        String nombres,
        String primerApellido,
        String segundoApellido,
        Integer idFirma,
        String nombreFirma,       // ej: "Bufete Jurídico Florez & Asociados"
        Integer idTipoCliente,
        String tipoCliente,       // ej: "Persona Natural"
        LocalDate fechaVinculacion,
        String correo,
        String telefonoMovil,
        LocalDate fechaNacimiento,
        boolean activo,

        // Ubicación y Contacto Detallado
        String lugarNacimiento,
        String nacionalidad,
        String ciudadResidencia,
        String localidadSector,
        String barrio,
        String direccionResidencia,
        String telefonoResidencia,
        String direccionOficina,
        String telefonoOficina,

        // Caracterización Detallada (IDs y Nombres para Catálogos/Selects)
        Integer idGenero,
        String genero,

        Integer idEstadoCivil,
        String estadoCivil,

        Integer idEstrato,
        String estrato,

        Integer idVivienda,
        String vivienda,

        Integer idNivelSalarial,
        String nivelSalarial,

        Integer idTipoEmpleado,
        String tipoEmpleado,

        Integer idGradoEscolaridad,
        String gradoEscolaridad,

        Integer idDiscapacidad,
        String discapacidad
) {}