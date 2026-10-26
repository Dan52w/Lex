package com.example.lexcloud.dto.response;

/** Resultado del alta transaccional de miembro (P1-F4). */
public record MiembroAltaResponseDTO(
        Integer idPersona,
        Integer idAbogado,
        String correo
) {}
