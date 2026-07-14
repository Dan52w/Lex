package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CasoRequestDTO(
        @NotNull(message = "El caso debe estar asociado a un Abogado o Persona de la firma")
        Integer idPersonaCreador,
        @NotNull(message = "El caso debe estar asociado a un cliente")
        Integer idCliente,
        @NotBlank(message = "El caso debe tener un titulo")
        String titulo,
        String descripcion,
        @Pattern(regexp = "^\\d{1,10}$", message = "El radicado interno debe tener un máximo de 10 dígitos numéricos")
        String radicadoInterno,
        @Pattern(regexp = "^\\d{23}$", message = "El radicado judicial debe tener exactamente 23 dígitos numéricos")
        String radicadoJudicial,
        @NotNull(message = "Debe haber un estado seleccionado para el caso")
        Integer idEstado,
        @NotNull(message = "Selecciona el tipo de caso")
        Integer idSubtipo,
        LocalDate fecha_Inicio,
        Integer idCasoPadre // Opcional
) {}