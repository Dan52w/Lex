package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CasoRequestDTO(
        @NotNull(message = "El caso debe estar asociado a un Abogado o Persona de la firma")
        Integer idPersonaCreador,
        @NotNull(message = "El caso debe estar asociado a un cliente")
        Integer idCliente,
        @NotBlank(message = "El caso debe tener un titulo")
        String titulo,
        String descripcion,
        @NotNull(message = "Debe haber un estado seleccionado para el caso")
        Integer idEstado,
        @NotNull(message = "Selecciona el tipo de caso")
        Integer idSubtipo,
        LocalDate fecha_Inicio,
        Integer idCasoPadre // Opcional
) {}
