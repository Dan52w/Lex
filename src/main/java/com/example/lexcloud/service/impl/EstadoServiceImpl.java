package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstadoMapper;
import com.example.lexcloud.dto.request.EstadoRequestDTO;
import com.example.lexcloud.dto.response.EstadoResponseDTO;
import com.example.lexcloud.entidades.Estado;
import com.example.lexcloud.repositorios.Estado_Repository;
import com.example.lexcloud.service.EstadoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EstadoServiceImpl implements EstadoService {
    private final Estado_Repository estadoRepository;
    private final EstadoMapper estadoMapper;

    public EstadoServiceImpl(Estado_Repository estadoRepository, EstadoMapper estadoMapper) {
        this.estadoRepository = estadoRepository;
        this.estadoMapper = estadoMapper;
    }

    @Override
    public EstadoResponseDTO guardar(EstadoRequestDTO request) {
        if (estadoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Estado con el nombre '" + request.nombre() + "' ya existe.");
        }

        Estado estado = estadoMapper.toEntity(request);
        try {
            return estadoMapper.toResponse(estadoRepository.save(estado));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Estado con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoResponseDTO> listarTodos() {
        return estadoMapper.toResponseList(estadoRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoResponseDTO> listarPorNombre(String nombre) {
        return estadoMapper.toResponseList(estadoRepository.findAllByNombre(nombre));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadoResponseDTO> buscarPorNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .map(estadoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadoResponseDTO> buscarPorId(Integer id) {
        return estadoRepository.findById(id)
                .map(estadoMapper::toResponse);
    }

    @Override
    public EstadoResponseDTO actualizar(Integer id, EstadoRequestDTO request) {
        return estadoRepository.findById(id).map(estado -> {
            // Actualizamos los campos necesarios
            estado.setNombre(request.nombre());

            return estadoMapper.toResponse(estadoRepository.save(estado));
        }).orElseThrow(() -> new RuntimeException("No se encontró el estado con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!estadoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Estado no encontrado");
        }
        estadoRepository.deleteById(id);
    }
}
