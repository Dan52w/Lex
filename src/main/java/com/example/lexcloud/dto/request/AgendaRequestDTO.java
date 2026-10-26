package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendaRequestDTO(
        // P1: opcional. Si viene null, el backend deriva el abogado del
        // token (CurrentUser.abogadoId()), igual que P0-B2 deriva el
        // creador de un caso del token. El front real (NuevaAgendaModal)
        // ya NO envía idAbogado, así que exigirlo aquí convertía todo
        // POST en 400 antes de llegar al servicio. Si viene informado
        // se sigue validando que sea de la firma del token.
        Integer idAbogado,
        // P1: explícito (antes era nullable sin anotación y el null
        // reventaba dentro de CurrentUser.requireCaso con una
        // IllegalArgumentException accidental). La entidad lo exige
        // NOT NULL, así que un null es 400 de validación, no 500/409.
        @NotNull(message = "Debe haber un Caso referenciado")
        Integer idCaso,
        @NotNull(message = "Debe seleccionar que tipo de evento es")
        Integer idTipoAgenda,
        Boolean activa,
        @NotNull(message = "Se debe seleccionar la fecha")
        @Future(message = "La fecha debe ser posterior a la actual")
        LocalDateTime fechaExp,
        // P1: la columna descripcion es NOT NULL en BD; un null llegaba
        // a DataIntegrityViolation (409) en vez de 400 de validación.
        @NotBlank(message = "Debe indicar una descripción")
        String descripcion
) {}
