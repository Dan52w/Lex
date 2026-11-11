package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoViviendaMapper;
import com.example.lexcloud.dto.request.TipoViviendaRequestDTO;
import com.example.lexcloud.dto.response.TipoViviendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Vivienda;
import com.example.lexcloud.repositorios.TipoVivienda_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoViviendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Lote 6 (M14): impl delgado — todo el flujo (normalización, duplicados,
 * vetos de seeds, borrado veraz, solo-admin, búsqueda parcial, paginación)
 * vive en {@link AbstractCatalogoService}.
 */
@Service
@Transactional
public class TipoViviendaServiceImpl extends AbstractCatalogoService<Tipo_Vivienda, TipoViviendaRequestDTO, TipoViviendaResponseDTO> implements TipoViviendaService {

    public TipoViviendaServiceImpl(TipoVivienda_Repository repositorio, TipoViviendaMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "PROPIA",
            "ARRENDADA",
            "FAMILIAR",
            "COMPARTIDA (ROOMIE)"), "Tipo Vivienda",
                TipoViviendaRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Vivienda::getNombre, Tipo_Vivienda::setNombre, Tipo_Vivienda::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<TipoViviendaResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<TipoViviendaResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
