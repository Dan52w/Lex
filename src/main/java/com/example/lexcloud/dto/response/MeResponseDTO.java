package com.example.lexcloud.dto.response;

/**
 * P0-F1: datos de PRESENTACIÓN del usuario autenticado, resueltos desde el
 * token en el servidor. El front los usa solo para mostrar nombre/rol; nunca
 * como fuente de autorización. No contiene ningún token ni credencial.
 * idFirma/idAbogado pueden ser null (p. ej. admin sin registro de abogado).
 */
public record MeResponseDTO(
        Integer personaId,
        String nombreCompleto,
        String correo,
        String rol,
        Integer idFirma,
        Integer idAbogado
) {}
