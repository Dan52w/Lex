package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotNull;

public record AbogadoRequestDTO(
        @NotNull(message = "El ID de persona es obligatorio")
        Integer idPersona,
        // P0-B2: idFirma OPCIONAL. Si es null, el backend lo deriva del
        // token del usuario autenticado (ver AbogadoServiceImpl.guardar).
        // Sin firma resoluble la creación se rechaza con 400.
        Integer idFirma,
        @NotNull(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado,
        @NotNull(message = "El abogado debe tener una tarjeta profesional")
        String tarjeta_Profesional
) {}