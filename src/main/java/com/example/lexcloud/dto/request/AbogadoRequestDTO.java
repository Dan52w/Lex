package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AbogadoRequestDTO(
        @NotNull(message = "El ID de persona es obligatorio")
        Integer idPersona,
        // P0-B2: idFirma OPCIONAL. Si es null, el backend lo deriva del
        // token del usuario autenticado (ver AbogadoServiceImpl.guardar).
        // Sin firma resoluble la creación se rechaza con 400.
        Integer idFirma,
        @NotNull(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado,
        // La columna no tiene length explícito (VARCHAR 255 por defecto);
        // el servicio ya hace trim + requerirNoBlank, el DTO lo refleja.
        @NotBlank(message = "El abogado debe tener una tarjeta profesional")
        @Size(max = 255, message = "La tarjeta profesional no puede superar los 255 caracteres")
        String tarjeta_Profesional
) {}