package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstadoCivilMapper;
import com.example.lexcloud.dto.request.EstadoCivilRequestDTO;
import com.example.lexcloud.dto.response.EstadoCivilResponseDTO;
import com.example.lexcloud.entidades.Estado_Civil;
import com.example.lexcloud.repositorios.EstadoCivil_Repository;
import com.example.lexcloud.service.EstadoCivilService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EstadoCivilServiceImpl implements EstadoCivilService {
    private final EstadoCivil_Repository estadoCivilRepository;
    private final EstadoCivilMapper estadoCivilMapper;

    public EstadoCivilServiceImpl(EstadoCivil_Repository estadoCivilRepository,
                                  EstadoCivilMapper estadoCivilMapper) {
        this.estadoCivilRepository = estadoCivilRepository;
        this.estadoCivilMapper = estadoCivilMapper;
    }

    @Override
    public EstadoCivilResponseDTO guardar(EstadoCivilRequestDTO request) {
        if (estadoCivilRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("Ya existe una discapacidad con el nombre: " + request.nombre());
        }

        Estado_Civil estadoCivil = estadoCivilMapper.toEntity(request);
        return estadoCivilMapper.toResponse(estadoCivilRepository.save(estadoCivil));
    }

    @Override
    public List<EstadoCivilResponseDTO> listarPorNombre(String nombre) {
        return estadoCivilMapper.toResponseList(estadoCivilRepository.findAllByNombre(nombre));
    }

    @Override
    public List<EstadoCivilResponseDTO> listarTodos() {
        return estadoCivilMapper.toResponseList(estadoCivilRepository.findAll());
    }

    @Override
    public Optional<EstadoCivilResponseDTO> buscarPorNombre(String nombre) {
        return estadoCivilRepository.findByNombre(nombre)
                .map(estadoCivilMapper::toResponse);
    }

    @Override
    public Optional<EstadoCivilResponseDTO> buscarPorId(Integer id) {
        return estadoCivilRepository.findById(id)
                .map(estadoCivilMapper::toResponse);
    }

    @Override
    public EstadoCivilResponseDTO actualizar(Integer id, EstadoCivilRequestDTO request) {
        return estadoCivilRepository.findById(id).map(estadoCivil -> {
            // Actualizamos los campos necesarios
            estadoCivil.setNombre(request.nombre());

            return estadoCivilMapper.toResponse(estadoCivilRepository.save(estadoCivil));
        }).orElseThrow(() -> new RuntimeException("No se encontró el estado civil con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!estadoCivilRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Estado Civil no encontrado");
        }
        estadoCivilRepository.deleteById(id);
    }
}
