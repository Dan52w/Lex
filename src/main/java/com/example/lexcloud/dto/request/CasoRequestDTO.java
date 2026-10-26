package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CasoRequestDTO(
        @NotNull(message = "El caso debe estar asociado a un Abogado o Persona de la firma")
        Integer idPersonaCreador,
        @NotNull(message = "El caso debe estar asociado a un cliente")
        Integer idCliente,
        @NotBlank(message = "El caso debe tener un titulo")
        @Size(max = 255, message = "El campo no puede superar 255 caracteres")
        String titulo,
        @Size(max = 5000, message = "El campo no puede superar 5000 caracteres")
        String descripcion,
        @Pattern(regexp = "^\\d{23}$", message = "El radicado judicial debe tener exactamente 23 dígitos numéricos")
        String radicadoJudicial,
        @NotNull(message = "Debe haber un estado seleccionado para el caso")
        Integer idEstado,
        @NotNull(message = "Selecciona el tipo de caso")
        Integer idSubtipo,
        LocalDate fecha_Inicio,
        @NotNull(message = "El caso debe estar referenciado a una firma")
        Integer idFirma,
        Integer idCasoPadre // Opcional
) {}