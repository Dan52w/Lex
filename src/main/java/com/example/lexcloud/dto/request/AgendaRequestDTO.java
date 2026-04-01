package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendaRequestDTO(
        @NotNull(message = "Debe haber un Abogado referenciado")
        Integer idAbogado,
        Integer idCaso,
        @NotNull(message = "Debe seleccionar que tipo de evento es")
        Integer idTipoAgenda,
        @NotBlank(message = "Se debe seleccionar la fecha en la que será el evento")
        LocalDateTime fechaExp,
        String descripcion
) {}
