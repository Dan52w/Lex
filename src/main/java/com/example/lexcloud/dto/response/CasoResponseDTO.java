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
        LocalDate fecha_Inicio,
        // El front (EditCasoModal) lee idCasoPadre para reenviarlo en el PUT;
        // idFirma/activo son campos propios de la entidad que faltaban.
        // idCliente e idPersonaCreador NO viven en Caso sino en el vínculo
        // 'Principal': los debe enriquecer el servicio (ver informe).
        Integer idFirma,
        Integer idCasoPadre,
        Boolean activo
) {}