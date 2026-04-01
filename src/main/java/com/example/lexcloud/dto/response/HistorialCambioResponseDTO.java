package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

public record HistorialCambioResponseDTO(Integer id,
                                         LocalDateTime fecha,
                                         String descripcion,
                                         Integer idCaso,
                                         String tituloCaso,
                                         String nombrePersona) {
}
