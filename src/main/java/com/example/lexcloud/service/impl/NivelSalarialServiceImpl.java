package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.NivelSalarialMapper;
import com.example.lexcloud.dto.request.NivelSalarialRequestDTO;
import com.example.lexcloud.dto.response.NivelSalarialResponseDTO;
import com.example.lexcloud.entidades.Nivel_Salarial;
import com.example.lexcloud.repositorios.NivelSalarial_Repository;
import com.example.lexcloud.service.NivelSalarialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NivelSalarialServiceImpl implements NivelSalarialService {
    private final NivelSalarial_Repository nivelSalarialRepository;
    private final NivelSalarialMapper nivelSalarialMapper;

    public NivelSalarialServiceImpl(NivelSalarial_Repository nivelSalarialRepository,
                                    NivelSalarialMapper nivelSalarialMapper) {
        this.nivelSalarialRepository = nivelSalarialRepository;
        this.nivelSalarialMapper = nivelSalarialMapper;
    }

    @Override
    public NivelSalarialResponseDTO guardar(NivelSalarialRequestDTO request) {
        if (nivelSalarialRepository.findByRango(request.rango()).isPresent()) {
            throw new IllegalStateException("Ya existe un nivel salarial con el nombre: " + request.rango());
        }

        Nivel_Salarial nivelSalarial = nivelSalarialMapper.toEntity(request);
        return nivelSalarialMapper.toResponse(nivelSalarialRepository.save(nivelSalarial));
    }

    @Override
    public List<NivelSalarialResponseDTO> listarPorRango(String rango) {
        return nivelSalarialMapper.toResponseList(nivelSalarialRepository.findAllByRango(rango));
    }

    @Override
    public List<NivelSalarialResponseDTO> listarTodos() {
        return nivelSalarialMapper.toResponseList(nivelSalarialRepository.findAll());
    }

    @Override
    public Optional<NivelSalarialResponseDTO> buscarPorRango(String rango) {
        return nivelSalarialRepository.findByRango(rango)
                .map(nivelSalarialMapper::toResponse);
    }

    @Override
    public Optional<NivelSalarialResponseDTO> buscarPorId(Integer id) {
        return nivelSalarialRepository.findById(id)
                .map(nivelSalarialMapper::toResponse);
    }

    @Override
    public NivelSalarialResponseDTO actualizar(Integer id, NivelSalarialRequestDTO request) {
        return nivelSalarialRepository.findById(id).map(nivelSalarial -> {
            // Actualizamos los campos necesarios
            nivelSalarial.setRango(request.rango());

            return nivelSalarialMapper.toResponse(nivelSalarialRepository.save(nivelSalarial));
        }).orElseThrow(() -> new RuntimeException("No se encontró el nivel salarial con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!nivelSalarialRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Nivel salarial no encontrado");
        }
        nivelSalarialRepository.deleteById(id);
    }
}
