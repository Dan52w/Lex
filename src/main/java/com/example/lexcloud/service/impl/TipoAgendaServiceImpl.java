package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoAgendaMapper;
import com.example.lexcloud.dto.request.TipoAgentaResquestDTO;
import com.example.lexcloud.dto.response.TipoAgendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Agenda;
import com.example.lexcloud.repositorios.TipoAgenda_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoAgendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Lote 6 (M14): impl delgado — todo el flujo (normalización, duplicados,
 * vetos de seeds, borrado veraz, solo-admin, búsqueda parcial, paginación)
 * vive en {@link AbstractCatalogoService}.
 */
@Service
@Transactional
public class TipoAgendaServiceImpl extends AbstractCatalogoService<Tipo_Agenda, TipoAgentaResquestDTO, TipoAgendaResponseDTO> implements TipoAgendaService {

    public TipoAgendaServiceImpl(TipoAgenda_Repository repositorio, TipoAgendaMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(), "Tipo Agenda",
                TipoAgentaResquestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Agenda::getNombre, Tipo_Agenda::setNombre, Tipo_Agenda::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }
}
