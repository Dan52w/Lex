package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.GeneroMapper;
import com.example.lexcloud.dto.request.GeneroRequestDTO;
import com.example.lexcloud.dto.response.GeneroResponseDTO;
import com.example.lexcloud.entidades.Genero;
import com.example.lexcloud.repositorios.Genero_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.GeneroService;
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
public class GeneroServiceImpl extends AbstractCatalogoService<Genero, GeneroRequestDTO, GeneroResponseDTO> implements GeneroService {

    public GeneroServiceImpl(Genero_Repository repositorio, GeneroMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "MASCULINO",
            "FEMENINO",
            "NO BINARIO",
            "PREFIERO NO DECIRLO"), "Genero",
                GeneroRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Genero::getNombre, Genero::setNombre, Genero::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<GeneroResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<GeneroResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
