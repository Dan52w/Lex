package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoEmpleadoMapper;
import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Empleado;
import com.example.lexcloud.repositorios.TipoEmpleado_Repository;
import com.example.lexcloud.service.TipoEmpleadoService;

import java.util.List;
import java.util.Optional;

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
            throw new IllegalStateException("Ya existe un Tipo empleado con el nombre: " + request.nombre());
        }

        Tipo_Empleado tipoEmpleado = tipoEmpleadoMapper.toEntity(request);
        return tipoEmpleadoMapper.toResponse(tipoEmpleadoRepository.save(tipoEmpleado));
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
            // Actualizamos los campos necesarios
            tipoEmpleado.setNombre(request.nombre());

            return tipoEmpleadoMapper.toResponse(tipoEmpleadoRepository.save(tipoEmpleado));
        }).orElseThrow(() -> new RuntimeException("No se encontró el tipo empleado con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!tipoEmpleadoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Tipo Empleado no encontrado");
        }
        tipoEmpleadoRepository.deleteById(id);
    }
}
