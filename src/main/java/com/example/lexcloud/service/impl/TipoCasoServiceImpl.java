package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoCasoMapper;
import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Caso;
import com.example.lexcloud.repositorios.TipoCaso_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoCasoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Lote 6 (M14): impl delgado — todo el flujo (normalización, duplicados,
 * vetos de seeds, borrado veraz, solo-admin, búsqueda parcial, paginación)
 * vive en {@link AbstractCatalogoService}.
 *
 * M16 — 'RAÍZ' queda oculto en listarTodo, listarPorNombre y buscarPorId
 * (además de vetado contra renombre y borrado por ser semilla). */
@Service
@Transactional
public class TipoCasoServiceImpl extends AbstractCatalogoService<Tipo_Caso, TipoCasoRequestDTO, TipoCasoResponseDTO> implements TipoCasoService {

    public TipoCasoServiceImpl(TipoCaso_Repository repositorio, TipoCasoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "RAÍZ"), "Tipo Caso",
                TipoCasoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Caso::getNombre, Tipo_Caso::setNombre, Tipo_Caso::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    protected boolean esVisible(Tipo_Caso entidad) {
        // M16: el tipo raíz del sistema no se lista ni se resuelve por id.
        return !"RAÍZ".equalsIgnoreCase(entidad.getNombre());
    }
}
