package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.DiscapacidadMapper;
import com.example.lexcloud.dto.request.DiscapacidadRequestDTO;
import com.example.lexcloud.dto.response.DiscapacidadResponseDTO;
import com.example.lexcloud.entidades.Discapacidad;
import com.example.lexcloud.repositorios.Discapacidad_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.DiscapacidadService;
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
public class DiscapacidadServiceImpl extends AbstractCatalogoService<Discapacidad, DiscapacidadRequestDTO, DiscapacidadResponseDTO> implements DiscapacidadService {

    public DiscapacidadServiceImpl(Discapacidad_Repository repositorio, DiscapacidadMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "NINGUNA",
            "FÍSICA / MOTORA",
            "VISUAL",
            "AUDITIVA",
            "COGNITIVA / MENTAL",
            "MÚLTIPLE"), "Discapacidad",
                DiscapacidadRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Discapacidad::getNombre, Discapacidad::setNombre, Discapacidad::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<DiscapacidadResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<DiscapacidadResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
