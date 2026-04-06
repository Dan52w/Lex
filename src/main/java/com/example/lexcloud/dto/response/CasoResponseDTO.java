package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record CasoResponseDTO(
        Integer id,
        String titulo,
        String descripcion,
        String estadoNombre,
        String subtipoNombre,
        LocalDate fecha_Inicio
) {}
