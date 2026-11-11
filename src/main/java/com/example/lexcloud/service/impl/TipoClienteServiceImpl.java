package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoClienteMapper;
import com.example.lexcloud.dto.request.TipoClienteRequestDTO;
import com.example.lexcloud.dto.response.TipoClienteResponseDTO;
import com.example.lexcloud.entidades.Tipo_Cliente;
import com.example.lexcloud.repositorios.TipoCliente_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoClienteService;
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
public class TipoClienteServiceImpl extends AbstractCatalogoService<Tipo_Cliente, TipoClienteRequestDTO, TipoClienteResponseDTO> implements TipoClienteService {

    public TipoClienteServiceImpl(TipoCliente_Repository repositorio, TipoClienteMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of("NATURAL", "JURIDICO"), "Tipo Cliente",
                TipoClienteRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Cliente::getNombre, Tipo_Cliente::setNombre, Tipo_Cliente::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }
}
