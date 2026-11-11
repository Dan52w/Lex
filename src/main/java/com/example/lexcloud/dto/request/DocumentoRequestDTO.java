package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentoRequestDTO(
        @NotNull(message = "El documento debe estar asociado a un caso")
        Integer idCaso,
        // @Size = columna documentos.nombre (@Size 255 en la entidad); el
        // servicio ya lo comprueba, esto da el 400 limpio de @Valid.
        @Size(max = 255, message = "El nombre del documento no debe superar los 255 caracteres")
        @NotBlank(message = "Es necesario un nombre para identificar el documento")
        String nombre,
        String descripcion,
        @NotNull(message = "Se debe indicar que tipo de documento es")
        Integer idTipoDocumento,
        @NotNull(message = "El documento debe estar asociado a una firma")
        String nitFirma) {
}
