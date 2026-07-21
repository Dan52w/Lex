package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoViviendaMapper;
import com.example.lexcloud.dto.request.TipoViviendaRequestDTO;
import com.example.lexcloud.dto.response.TipoViviendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Vivienda;
import com.example.lexcloud.repositorios.TipoVivienda_Repository;
import com.example.lexcloud.service.TipoViviendaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoViviendaServiceImpl implements TipoViviendaService {
    private final TipoVivienda_Repository tipoViviendaRepository;
    private final TipoViviendaMapper tipoViviendaMapper;

    public TipoViviendaServiceImpl(TipoVivienda_Repository tipoViviendaRepository,
                                   TipoViviendaMapper tipoViviendaMapper) {
        this.tipoViviendaRepository = tipoViviendaRepository;
        this.tipoViviendaMapper = tipoViviendaMapper;
    }

    @Override
    public TipoViviendaResponseDTO guardar(TipoViviendaRequestDTO request) {
        if (tipoViviendaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Vivienda con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Vivienda tipoVivienda = tipoViviendaMapper.toEntity(request);
        try {
            return tipoViviendaMapper.toResponse(tipoViviendaRepository.save(tipoVivienda));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Vivienda con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoViviendaResponseDTO> listarPorNombre(String nombre) {
        return tipoViviendaMapper.toResponseList(tipoViviendaRepository.findAllByNombre(nombre));
    }

    @Override
    public List<TipoViviendaResponseDTO> listarTodos() {
        return tipoViviendaMapper.toResponseList(tipoViviendaRepository.findAll());
    }

    @Override
    public Optional<TipoViviendaResponseDTO> buscarPorNombre(String nombre) {
        return tipoViviendaRepository.findByNombre(nombre)
                .map(tipoViviendaMapper::toResponse);
    }

    @Override
    public Optional<TipoViviendaResponseDTO> buscarPorId(Integer id) {
        return tipoViviendaRepository.findById(id)
                .map(tipoViviendaMapper::toResponse);
    }

    @Override
    public TipoViviendaResponseDTO actualizar(Integer id, TipoViviendaRequestDTO request) {
        return tipoViviendaRepository.findById(id).map(tipoVivienda -> {
            // Actualizamos los campos necesarios
            tipoVivienda.setNombre(request.nombre());

            return tipoViviendaMapper.toResponse(tipoViviendaRepository.save(tipoVivienda));
        }).orElseThrow(() -> new RuntimeException("No se encontró el tipo vivienda con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!tipoViviendaRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Tipo vivienda no encontrado");
        }
        tipoViviendaRepository.deleteById(id);
    }
}
