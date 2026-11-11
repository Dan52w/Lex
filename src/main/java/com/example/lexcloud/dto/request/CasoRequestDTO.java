package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CasoRequestDTO(
        // P0-B2: idPersonaCreador e idFirma OPCIONALES. El creador del
        // vínculo 'Principal' es siempre el usuario autenticado (ver
        // CasoServiceImpl.guardar) y la firma se deriva del token; si no
        // hay firma resoluble la creación se rechaza con 400.
        Integer idPersonaCreador,
        @NotNull(message = "El caso debe estar asociado a un cliente")
        Integer idCliente,
        @NotBlank(message = "El caso debe tener un titulo")
        @Size(max = 255, message = "El título no puede superar los 255 caracteres")
        String titulo,
        @NotBlank(message = "El caso debe tener una descripcion")
        String descripcion,
        @Pattern(regexp = "^\\d{23}$", message = "El radicado judicial debe tener exactamente 23 dígitos numéricos")
        String radicadoJudicial,
        @NotNull(message = "Debe haber un estado seleccionado para el caso")
        Integer idEstado,
        @NotNull(message = "Selecciona el tipo de caso")
        Integer idSubtipo,
        @NotNull(message = "El caso debe tener una fecha de inicio")
        LocalDate fecha_Inicio,
        // P0-B2: idFirma OPCIONAL. Si es null, el backend lo deriva del
        // token del usuario autenticado (ver AbogadoServiceImpl.guardar).
        // Sin firma resoluble la creación se rechaza con 400.
        Integer idFirma,
        Integer idCasoPadre // Opcional
) {}