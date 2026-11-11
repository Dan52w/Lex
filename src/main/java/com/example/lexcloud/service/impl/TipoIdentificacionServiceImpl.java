package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoIdentificacionMapper;
import com.example.lexcloud.dto.request.TipoIdentificacionRequestDTO;
import com.example.lexcloud.dto.response.TipoIdenficacionResponseDTO;
import com.example.lexcloud.entidades.Tipo_Identificacion;
import com.example.lexcloud.repositorios.TipoIdentificacion_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoIdentificacionService;
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
public class TipoIdentificacionServiceImpl extends AbstractCatalogoService<Tipo_Identificacion, TipoIdentificacionRequestDTO, TipoIdenficacionResponseDTO> implements TipoIdentificacionService {

    public TipoIdentificacionServiceImpl(TipoIdentificacion_Repository repositorio, TipoIdentificacionMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "CÉDULA DE CIUDADANÍA",
            "CÉDULA DE EXTRANJERÍA",
            "PASAPORTE",
            "NIT"), "Tipo Identificacion",
                TipoIdentificacionRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Identificacion::getNombre, Tipo_Identificacion::setNombre, Tipo_Identificacion::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<TipoIdenficacionResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<TipoIdenficacionResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
