package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.GeneroMapper;
import com.example.lexcloud.dto.request.GeneroRequestDTO;
import com.example.lexcloud.dto.response.GeneroResponseDTO;
import com.example.lexcloud.entidades.Genero;
import com.example.lexcloud.repositorios.Genero_Repository;
import com.example.lexcloud.service.GeneroService;

import java.util.List;
import java.util.Optional;

public class GeneroServiceImpl implements GeneroService {
    private final Genero_Repository generoRepository;
    private final GeneroMapper generoMapper;

    public GeneroServiceImpl(Genero_Repository generoRepository,
                             GeneroMapper generoMapper) {
        this.generoRepository = generoRepository;
        this.generoMapper = generoMapper;
    }

    @Override
    public GeneroResponseDTO guardar(GeneroRequestDTO request) {
        if (generoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("Ya existe un genero con el nombre: " + request.nombre());
        }

        Genero genero = generoMapper.toEntity(request);
        return generoMapper.toResponse(generoRepository.save(genero));
    }

    @Override
    public List<GeneroResponseDTO> listarPorNombre(String nombre) {
        return generoMapper.toResponseList(generoRepository.findAllByNombre(nombre));
    }

    @Override
    public List<GeneroResponseDTO> listarTodos() {
        return generoMapper.toResponseList(generoRepository.findAll());
    }

    @Override
    public Optional<GeneroResponseDTO> buscarPorNombre(String nombre) {
        return generoRepository.findByNombre(nombre)
                .map(generoMapper::toResponse);
    }

    @Override
    public Optional<GeneroResponseDTO> buscarPorId(Integer id) {
        return generoRepository.findById(id)
                .map(generoMapper::toResponse);
    }

    @Override
    public GeneroResponseDTO actualizar(Integer id, GeneroRequestDTO request) {
        return generoRepository.findById(id).map(genero -> {
            // Actualizamos los campos necesarios
            genero.setNombre(request.nombre());

            return generoMapper.toResponse(generoRepository.save(genero));
        }).orElseThrow(() -> new RuntimeException("No se encontró el genero con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!generoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Genero no encontrado");
        }
        generoRepository.deleteById(id);
    }
}
