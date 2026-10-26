package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

// P1: se añade idAbogado al final (campo aditivo; no rompe a los
// clientes que ya leen los demás campos por nombre). Sin él, el
// front de edición (EditAgendaModal) leía data.idAbogado como
// undefined y reenviaba idAbogado=0 en el PUT, lo que daba 404
// ("Abogado no Encontrado") en cada actualización.
public record AgendaResponseDTO(Integer id,
                                Integer idTipoAgenda,
                                String tipoAgenda,
                                Boolean activa,
                                Integer idCaso,
                                String nombreCaso,
                                String descripcion,
                                LocalDateTime fechaExp,
                                Integer idAbogado) {
}
