package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoMapper;
import com.example.lexcloud.dto.request.VinculoRequestDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;
import com.example.lexcloud.entidades.Vinculo;
import com.example.lexcloud.repositorios.Vinculo_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.VinculoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Lote 6 (M14): impl delgado — todo el flujo (normalización, duplicados,
 * vetos de seeds, borrado veraz, solo-admin, búsqueda parcial, paginación)
 * vive en {@link AbstractCatalogoService}.
 *
 * M16 — SIN VÍNCULO y Principal NO se filtran: son valores seleccionables
 * por diseño y deben seguir visibles en los listados (solo están vetados
 * contra renombre y borrado por ser semillas). */
@Service
@Transactional
public class VinculoServiceImpl extends AbstractCatalogoService<Vinculo, VinculoRequestDTO, VinculoResponseDTO> implements VinculoService {

    public VinculoServiceImpl(Vinculo_Repository repositorio, VinculoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "SIN VÍNCULO",
            "PRINCIPAL"), "Vinculo",
                VinculoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Vinculo::getNombre, Vinculo::setNombre, Vinculo::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }
}
