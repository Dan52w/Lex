package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoEmpleadoMapper;
import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Empleado;
import com.example.lexcloud.repositorios.TipoEmpleado_Repository;
import com.example.lexcloud.service.TipoEmpleadoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoEmpleadoServiceImpl implements TipoEmpleadoService {
    private final TipoEmpleado_Repository tipoEmpleadoRepository;
    private final TipoEmpleadoMapper tipoEmpleadoMapper;

    public TipoEmpleadoServiceImpl(TipoEmpleado_Repository tipoEmpleadoRepository,
                                   TipoEmpleadoMapper tipoEmpleadoMapper) {
        this.tipoEmpleadoRepository = tipoEmpleadoRepository;
        this.tipoEmpleadoMapper = tipoEmpleadoMapper;
    }

    @Override
    public TipoEmpleadoResponseDTO guardar(TipoEmpleadoRequestDTO request) {
        if (tipoEmpleadoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Empleado con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Empleado tipoEmpleado = tipoEmpleadoMapper.toEntity(request);
        try {
            return tipoEmpleadoMapper.toResponse(tipoEmpleadoRepository.save(tipoEmpleado));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Empleado con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoEmpleadoResponseDTO> listarPorNombre(String nombre) {
        return tipoEmpleadoMapper.toResponseList(tipoEmpleadoRepository.findAllByNombre(nombre));
    }

    @Override
    public List<TipoEmpleadoResponseDTO> listarTodos() {
        return tipoEmpleadoMapper.toResponseList(tipoEmpleadoRepository.findAll());
    }

    @Override
    public Optional<TipoEmpleadoResponseDTO> buscarPorNombre(String nombre) {
        return tipoEmpleadoRepository.findByNombre(nombre)
                .map(tipoEmpleadoMapper::toResponse);
    }

    @Override
    public Optional<TipoEmpleadoResponseDTO> buscarPorId(Integer id) {
        return tipoEmpleadoRepository.findById(id)
                .map(tipoEmpleadoMapper::toResponse);
    }

    @Override
    public TipoEmpleadoResponseDTO actualizar(Integer id, TipoEmpleadoRequestDTO request) {
        return tipoEmpleadoRepository.findById(id).map(tipoEmpleado -> {
            tipoEmpleadoRepository.findByNombre(request.nombre())
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new IllegalStateException("El Tipo Empleado con el nombre '" + request.nombre() + "' ya existe.");
                    });
            tipoEmpleado.setNombre(request.nombre());

            try {
                return tipoEmpleadoMapper.toResponse(tipoEmpleadoRepository.save(tipoEmpleado));
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalStateException("El Tipo Empleado con el nombre '" + request.nombre() + "' ya existe.");
            }
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo empleado con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!tipoEmpleadoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Tipo Empleado no encontrado");
        }
        tipoEmpleadoRepository.deleteById(id);
    }
}
