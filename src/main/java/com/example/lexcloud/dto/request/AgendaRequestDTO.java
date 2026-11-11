package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendaRequestDTO(
        @NotNull(message = "Debe haber un Abogado referenciado")
        Integer idAbogado,
        @NotNull(message = "Debe haber un Caso referenciado")
        Integer idCaso,
        @NotNull(message = "Debe seleccionar que tipo de evento es")
        Integer idTipoAgenda,
        Boolean activa,
        // Sin @Future: la regla "fecha futura" vive en el servicio, porque
        // depende del contexto (P4/P10): en crear la fecha siempre debe ser
        // futura; en actualizar solo si CAMBIÓ. Con @Future aquí, editar
        // solo la descripción de una agenda vencida daba 400 antes de
        // llegar al servicio (el front reenvía la fecha actual del modal).
        @NotNull(message = "Se debe seleccionar la fecha")
        LocalDateTime fechaExp,
        String descripcion
) {}
