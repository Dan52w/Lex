package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoAbogadoMapper;
import com.example.lexcloud.dto.request.TipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.TipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Abogado;
import com.example.lexcloud.repositorios.TipoAbogado_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoAbogadoService;
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
public class TipoAbogadoServiceImpl extends AbstractCatalogoService<Tipo_Abogado, TipoAbogadoRequestDTO, TipoAbogadoResponseDTO> implements TipoAbogadoService {

    public TipoAbogadoServiceImpl(TipoAbogado_Repository repositorio, TipoAbogadoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(), "Tipo Abogado",
                TipoAbogadoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Abogado::getNombre, Tipo_Abogado::setNombre, Tipo_Abogado::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }
}
