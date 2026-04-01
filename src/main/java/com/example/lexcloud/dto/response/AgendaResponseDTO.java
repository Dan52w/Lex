package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

public record AgendaResponseDTO(Integer id,
                                String tipoAgenda,
                                Boolean activa,
                                String nombreCaso,
                                String descrpcion,
                                LocalDateTime fechaExp) {
}
