package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.NivelSalarialMapper;
import com.example.lexcloud.dto.request.NivelSalarialRequestDTO;
import com.example.lexcloud.dto.response.NivelSalarialResponseDTO;
import com.example.lexcloud.entidades.Nivel_Salarial;
import com.example.lexcloud.repositorios.NivelSalarial_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.NivelSalarialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lote 6 (M14): impl delgado — todo el flujo (normalización, duplicados,
 * vetos de seeds, borrado veraz, solo-admin, búsqueda parcial, paginación)
 * vive en {@link AbstractCatalogoService}.
 */
@Service
@Transactional
public class NivelSalarialServiceImpl extends AbstractCatalogoService<Nivel_Salarial, NivelSalarialRequestDTO, NivelSalarialResponseDTO> implements NivelSalarialService {

    public NivelSalarialServiceImpl(NivelSalarial_Repository repositorio, NivelSalarialMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "MENOS DE 1 SMLV",
            "1 - 2 SMLV",
            "3 - 5 SMLV",
            "MÁS DE 5 SMLV"), "Nivel Salarial",
                NivelSalarialRequestDTO::rango, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Nivel_Salarial::getRango, Nivel_Salarial::setRango, Nivel_Salarial::getId,
                repositorio::findByRangoIgnoreCase, repositorio::buscarPorRangoParcial, repositorio::buscarPorRangoParcial);
    }

    @Override
    public List<NivelSalarialResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<NivelSalarialResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }

    @Override
    public List<NivelSalarialResponseDTO> listarPorRango(String rango) {
        return super.listarPorNombre(rango);
    }

    @Override
    public List<NivelSalarialResponseDTO> listarPorRango(String rango, int page, int size) {
        return super.listarPorNombre(rango, page, size);
    }

    @Override
    public Optional<NivelSalarialResponseDTO> buscarPorRango(String rango) {
        return super.buscarPorNombre(rango);
    }
}
