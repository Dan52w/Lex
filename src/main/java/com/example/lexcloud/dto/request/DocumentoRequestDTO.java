package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequestDTO(
        @NotNull(message = "El documento debe estar asociado a un caso")
        Integer idCaso,
        @NotBlank(message = "Es necesario un nombre para identificar el documento")
        String nombre,
        String descripcion,
        @NotNull(message = "Se debe indicar que tipo de documento es")
        Integer idTipoDocumento,
        @NotNull(message = "El documento debe estar asociado a una firma")
        String nitFirma) {
}
