package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoRelacionEquipoMapper;
import com.example.lexcloud.dto.request.TipoRelacionEquipoRequestDTO;
import com.example.lexcloud.dto.response.TipoRelacionEquipoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Relacion_Equipo;
import com.example.lexcloud.repositorios.TipoRelacionEquipo_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoRelacionEquipoService;
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
public class TipoRelacionEquipoServiceImpl extends AbstractCatalogoService<Tipo_Relacion_Equipo, TipoRelacionEquipoRequestDTO, TipoRelacionEquipoResponseDTO> implements TipoRelacionEquipoService {

    public TipoRelacionEquipoServiceImpl(TipoRelacionEquipo_Repository repositorio, TipoRelacionEquipoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "SECRETARIO(A)",
            "ASISTENTE LEGAL",
            "APRENDIZ / JUDICANTE",
            "CO-ABOGADO JUNIOR"), "Tipo Relacion Equipo",
                TipoRelacionEquipoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Relacion_Equipo::getNombre, Tipo_Relacion_Equipo::setNombre, Tipo_Relacion_Equipo::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<TipoRelacionEquipoResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<TipoRelacionEquipoResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
