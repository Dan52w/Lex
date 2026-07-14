package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record ClienteResponseDTO(
        Integer id,
        String identificacion,
        String tipoIdentificacion, // de tipoIdentificacion.nombre
        String nombres,
        String primerApellido,
        String segundoApellido,
        String nombreFirma,       // de firma.nombre
        String tipoCliente,       // de tipoCliente.nombre
        LocalDate fechaVinculacion,
        String correo,
        String telefonoMovil,
        LocalDate fechaNacimiento,
        boolean activo
) {}