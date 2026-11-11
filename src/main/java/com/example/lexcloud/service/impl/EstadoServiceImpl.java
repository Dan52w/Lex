package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstadoMapper;
import com.example.lexcloud.dto.request.EstadoRequestDTO;
import com.example.lexcloud.dto.response.EstadoResponseDTO;
import com.example.lexcloud.entidades.Estado;
import com.example.lexcloud.repositorios.Estado_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.EstadoService;
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
public class EstadoServiceImpl extends AbstractCatalogoService<Estado, EstadoRequestDTO, EstadoResponseDTO> implements EstadoService {

    public EstadoServiceImpl(Estado_Repository repositorio, EstadoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "SIN DEFINIR"), "Estado",
                EstadoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Estado::getNombre, Estado::setNombre, Estado::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<EstadoResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<EstadoResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
