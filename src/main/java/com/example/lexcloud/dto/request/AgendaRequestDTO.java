package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendaRequestDTO(
        @NotNull(message = "Debe haber un Abogado referenciado")
        Integer idAbogado,
        Integer idCaso,
        @NotNull(message = "Debe seleccionar que tipo de evento es")
        Integer idTipoAgenda,
        Boolean activa,
        @NotNull(message = "Se debe seleccionar la fecha")
        @Future(message = "La fecha debe ser posterior a la actual")
        LocalDateTime fechaExp,
        String descripcion
) {}
