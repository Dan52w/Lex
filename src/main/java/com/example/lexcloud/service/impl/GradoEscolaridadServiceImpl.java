package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.GradoEscolaridadMapper;
import com.example.lexcloud.dto.request.GradoEscolaridadRequestDTO;
import com.example.lexcloud.dto.response.GradoEscolaridadResponseDTO;
import com.example.lexcloud.entidades.Grado_Escolaridad;
import com.example.lexcloud.repositorios.GradoEscolaridad_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.GradoEscolaridadService;
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
public class GradoEscolaridadServiceImpl extends AbstractCatalogoService<Grado_Escolaridad, GradoEscolaridadRequestDTO, GradoEscolaridadResponseDTO> implements GradoEscolaridadService {

    public GradoEscolaridadServiceImpl(GradoEscolaridad_Repository repositorio, GradoEscolaridadMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "PRIMARIA",
            "SECUNDARIA / BACHILLERATO",
            "TÉCNICO / TECNÓLOGO",
            "UNIVERSITARIO",
            "POSTGRADO"), "Grado Escolaridad",
                GradoEscolaridadRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Grado_Escolaridad::getNombre, Grado_Escolaridad::setNombre, Grado_Escolaridad::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<GradoEscolaridadResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<GradoEscolaridadResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
