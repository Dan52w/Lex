package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record CasoResponseDTO(
        Integer id,
        String titulo,
        String descripcion,
        String estadoNombre,
        Integer idEstado,
        String subtipoNombre,
        Integer idSubtipo,
        String radicadoInterno,
        String radicadoJudicial,
        LocalDate fecha_Inicio
) {}