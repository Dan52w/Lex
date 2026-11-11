package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoDocumentoMapper;
import com.example.lexcloud.dto.request.TipoDocumentoRequestDTO;
import com.example.lexcloud.dto.response.TipoDocumentoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Documento;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoDocumentoService;
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
public class TipoDocumentoServiceImpl extends AbstractCatalogoService<Tipo_Documento, TipoDocumentoRequestDTO, TipoDocumentoResponseDTO> implements TipoDocumentoService {

    public TipoDocumentoServiceImpl(TipoDocumento_Repository repositorio, TipoDocumentoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(), "Tipo Documento",
                TipoDocumentoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Documento::getNombre, Tipo_Documento::setNombre, Tipo_Documento::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }
}
