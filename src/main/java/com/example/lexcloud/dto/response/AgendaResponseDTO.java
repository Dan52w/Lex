package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

public record AgendaResponseDTO(Integer id,
                                Integer idTipoAgenda,
                                String tipoAgenda,
                                Boolean activa,
                                Integer idCaso,
                                String nombreCaso,
                                String descripcion,
                                LocalDateTime fechaExp) {
}
