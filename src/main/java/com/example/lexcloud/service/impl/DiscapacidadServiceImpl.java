package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.DiscapacidadMapper;
import com.example.lexcloud.dto.request.DiscapacidadRequestDTO;
import com.example.lexcloud.dto.response.DiscapacidadResponseDTO;
import com.example.lexcloud.entidades.Discapacidad;
import com.example.lexcloud.repositorios.Discapacidad_Repository;
import com.example.lexcloud.service.DiscapacidadService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DiscapacidadServiceImpl implements DiscapacidadService {
    private final Discapacidad_Repository discapacidadRepository;
    private final DiscapacidadMapper discapacidadMapper;

    public DiscapacidadServiceImpl(Discapacidad_Repository discapacidadRepository,
                                   DiscapacidadMapper discapacidadMapper) {
        this.discapacidadRepository = discapacidadRepository;
        this.discapacidadMapper = discapacidadMapper;
    }

    @Override
    public DiscapacidadResponseDTO guardar(DiscapacidadRequestDTO request) {
        if (discapacidadRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("La discapacidad con el nombre '" + request.nombre() + "' ya existe.");
        }

        Discapacidad discapacidad = discapacidadMapper.toEntity(request);
        try {
            return discapacidadMapper.toResponse(discapacidadRepository.save(discapacidad));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("La discapacidad con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<DiscapacidadResponseDTO> listarPorNombre(String nombre) {
        return discapacidadMapper.toResponseList(discapacidadRepository.findAllByNombre(nombre));
    }

    @Override
    public List<DiscapacidadResponseDTO> listarTodos() {
        return discapacidadMapper.toResponseList(discapacidadRepository.findAll());
    }

    @Override
    public Optional<DiscapacidadResponseDTO> buscarPorNombre(String nombre) {
        return discapacidadRepository.findByNombre(nombre)
                .map(discapacidadMapper::toResponse);
    }

    @Override
    public Optional<DiscapacidadResponseDTO> buscarPorId(Integer id) {
        return discapacidadRepository.findById(id)
                .map(discapacidadMapper::toResponse);
    }

    @Override
    public DiscapacidadResponseDTO actualizar(Integer id, DiscapacidadRequestDTO request) {
        return discapacidadRepository.findById(id).map(discapacidad -> {
            // Actualizamos los campos necesarios
            discapacidad.setNombre(request.nombre());

            return discapacidadMapper.toResponse(discapacidadRepository.save(discapacidad));
        }).orElseThrow(() -> new RuntimeException("No se encontró el discapacidad con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!discapacidadRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Discapacidad no encontrado");
        }
        discapacidadRepository.deleteById(id);
    }
}
