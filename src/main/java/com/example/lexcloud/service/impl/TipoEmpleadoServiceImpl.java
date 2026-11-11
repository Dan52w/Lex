package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoEmpleadoMapper;
import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Empleado;
import com.example.lexcloud.repositorios.TipoEmpleado_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.TipoEmpleadoService;
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
public class TipoEmpleadoServiceImpl extends AbstractCatalogoService<Tipo_Empleado, TipoEmpleadoRequestDTO, TipoEmpleadoResponseDTO> implements TipoEmpleadoService {

    public TipoEmpleadoServiceImpl(TipoEmpleado_Repository repositorio, TipoEmpleadoMapper mapper, ContextoSeguridad contextoSeguridad) {
        super(repositorio, contextoSeguridad, Set.of(
            "INDEPENDIENTE",
            "EMPLEADO DEPENDIENTE",
            "DESEMPLEADO",
            "ESTUDIANTE",
            "PENSIONADO"), "Tipo Empleado",
                TipoEmpleadoRequestDTO::nombre, mapper::toEntity, mapper::toResponse, mapper::toResponseList,
                Tipo_Empleado::getNombre, Tipo_Empleado::setNombre, Tipo_Empleado::getId,
                repositorio::findByNombreIgnoreCase, repositorio::buscarPorNombreParcial, repositorio::buscarPorNombreParcial);
    }

    @Override
    public List<TipoEmpleadoResponseDTO> listarTodos() {
        return super.listarTodo();
    }

    @Override
    public List<TipoEmpleadoResponseDTO> listarTodos(int page, int size) {
        return super.listarTodo(page, size);
    }
}
