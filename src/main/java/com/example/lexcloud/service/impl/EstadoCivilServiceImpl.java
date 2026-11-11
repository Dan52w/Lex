package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstadoCivilMapper;
import com.example.lexcloud.dto.request.EstadoCivilRequestDTO;
import com.example.lexcloud.dto.response.EstadoCivilResponseDTO;
import com.example.lexcloud.entidades.Estado_Civil;
import com.example.lexcloud.repositorios.EstadoCivil_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.EstadoCivilService;
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
public class EstadoCivilServiceImpl extends AbstractCatalogoService<Estado_Civil, EstadoCivilRequestDTO, EstadoCivilResponseDTO> implements EstadoCivilService {

    public EstadoCivilServiceImpl(EstadoCivil_Repository repositorio, EstadoCivilMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "SOLTERO(A)",
            "CASADO(A)",
            "UNIÓN LIBRE",
            "DIVORCIADO(A)",
            "VIUDO(A)"), "Estado Civil",
                EstadoCivilRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Estado_Civil::getNombre, Estado_Civil::setNombre, Estado_Civil::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<EstadoCivilResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<EstadoCivilResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
