package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.GradoEscolaridadMapper;
import com.example.lexcloud.dto.request.GradoEscolaridadRequestDTO;
import com.example.lexcloud.dto.response.GradoEscolaridadResponseDTO;
import com.example.lexcloud.entidades.Grado_Escolaridad;
import com.example.lexcloud.repositorios.GradoEscolaridad_Repository;
import com.example.lexcloud.service.GradoEscolaridadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GradoEscolaridadServiceImpl implements GradoEscolaridadService {
    private final GradoEscolaridad_Repository gradoEscolaridadRepository;
    private final GradoEscolaridadMapper gradoEscolaridadMapper;

    public GradoEscolaridadServiceImpl(GradoEscolaridad_Repository gradoEscolaridadRepository,
                                       GradoEscolaridadMapper gradoEscolaridadMapper) {
        this.gradoEscolaridadRepository = gradoEscolaridadRepository;
        this.gradoEscolaridadMapper = gradoEscolaridadMapper;
    }

    @Override
    public GradoEscolaridadResponseDTO guardar(GradoEscolaridadRequestDTO request) {
        if (gradoEscolaridadRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("Ya existe un Grado de Esocolaridad con el nombre: " + request.nombre());
        }

        Grado_Escolaridad gradoEscolaridad = gradoEscolaridadMapper.toEntity(request);
        return gradoEscolaridadMapper.toResponse(gradoEscolaridadRepository.save(gradoEscolaridad));
    }

    @Override
    public List<GradoEscolaridadResponseDTO> listarPorNombre(String nombre) {
        return gradoEscolaridadMapper.toResponseList(gradoEscolaridadRepository.findAllByNombre(nombre));
    }

    @Override
    public List<GradoEscolaridadResponseDTO> listarTodos() {
        return gradoEscolaridadMapper.toResponseList(gradoEscolaridadRepository.findAll());
    }

    @Override
    public Optional<GradoEscolaridadResponseDTO> buscarPorNombre(String nombre) {
        return gradoEscolaridadRepository.findByNombre(nombre)
                .map(gradoEscolaridadMapper::toResponse);
    }

    @Override
    public Optional<GradoEscolaridadResponseDTO> buscarPorId(Integer id) {
        return gradoEscolaridadRepository.findById(id)
                .map(gradoEscolaridadMapper::toResponse);
    }

    @Override
    public GradoEscolaridadResponseDTO actualizar(Integer id, GradoEscolaridadRequestDTO request) {
        return gradoEscolaridadRepository.findById(id).map(gradoEscolaridad -> {
            // Actualizamos los campos necesarios
            gradoEscolaridad.setNombre(request.nombre());

            return gradoEscolaridadMapper.toResponse(gradoEscolaridadRepository.save(gradoEscolaridad));
        }).orElseThrow(() -> new RuntimeException("No se encontró el grado de escolaridad con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!gradoEscolaridadRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Grado de Escolaridad no encontrado");
        }
        gradoEscolaridadRepository.deleteById(id);
    }
}
