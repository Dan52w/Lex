package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstratoEconomicoMapper;
import com.example.lexcloud.dto.request.EstratoEconomicoRequestDTO;
import com.example.lexcloud.dto.response.EstratoEconomicoResponseDTO;
import com.example.lexcloud.entidades.Estrato_Economico;
import com.example.lexcloud.repositorios.EstratoEconomico_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.EstratoEconomicoService;
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
public class EstratoEconomicoServiceImpl extends AbstractCatalogoService<Estrato_Economico, EstratoEconomicoRequestDTO, EstratoEconomicoResponseDTO> implements EstratoEconomicoService {

    public EstratoEconomicoServiceImpl(EstratoEconomico_Repository repositorio, EstratoEconomicoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "ESTRATO 1",
            "ESTRATO 2",
            "ESTRATO 3",
            "ESTRATO 4",
            "ESTRATO 5",
            "ESTRATO 6"), "Estrato Economico",
                EstratoEconomicoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Estrato_Economico::getNombre, Estrato_Economico::setNombre, Estrato_Economico::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<EstratoEconomicoResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<EstratoEconomicoResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
