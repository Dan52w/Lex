package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CasoRequestDTO(
        @NotBlank(message = "El caso debe tener un titulo")
        String titulo,
        String descripcion,
        @NotNull(message = "Debe haber un estado seleccionado para el caso")
        Integer idEstado,
        @NotNull(message = "Selecciona el tipo de caso")
        Integer idSubTipo,
        Integer idCasoPadre // Opcional
) {}
