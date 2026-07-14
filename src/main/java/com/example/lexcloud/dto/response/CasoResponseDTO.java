package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record CasoResponseDTO(
        Integer id,
        String titulo,
        String descripcion,
        String estadoNombre,
        String subtipoNombre,
        String radicadoInterno,
        String radicadoJudicial,
        LocalDate fecha_Inicio
) {}