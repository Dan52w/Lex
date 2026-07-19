package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EstratoEconomicoMapper;
import com.example.lexcloud.dto.request.EstratoEconomicoRequestDTO;
import com.example.lexcloud.dto.response.EstratoEconomicoResponseDTO;
import com.example.lexcloud.entidades.Estrato_Economico;
import com.example.lexcloud.repositorios.EstratoEconomico_Repository;
import com.example.lexcloud.service.EstratoEconomicoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EstratoEconomicoServiceImpl implements EstratoEconomicoService {
    private final EstratoEconomico_Repository estratoEconomicoRepository;
    private final EstratoEconomicoMapper estratoEconomicoMapper;

    public EstratoEconomicoServiceImpl(EstratoEconomico_Repository estratoEconomicoRepository,
                                       EstratoEconomicoMapper estratoEconomicoMapper) {
        this.estratoEconomicoRepository = estratoEconomicoRepository;
        this.estratoEconomicoMapper = estratoEconomicoMapper;
    }

    @Override
    public EstratoEconomicoResponseDTO guardar(EstratoEconomicoRequestDTO request) {
        if (estratoEconomicoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("Ya existe una discapacidad con el nombre: " + request.nombre());
        }

        Estrato_Economico estratoEconomico = estratoEconomicoMapper.toEntity(request);
        return estratoEconomicoMapper.toResponse(estratoEconomicoRepository.save(estratoEconomico));
    }

    @Override
    public List<EstratoEconomicoResponseDTO> listarPorNombre(String nombre) {
        return estratoEconomicoMapper.toResponseList(estratoEconomicoRepository.findAllByNombre(nombre));
    }

    @Override
    public List<EstratoEconomicoResponseDTO> listarTodos() {
        return estratoEconomicoMapper.toResponseList(estratoEconomicoRepository.findAll());
    }

    @Override
    public Optional<EstratoEconomicoResponseDTO> buscarPorNombre(String nombre) {
        return estratoEconomicoRepository.findByNombre(nombre)
                .map(estratoEconomicoMapper::toResponse);
    }

    @Override
    public Optional<EstratoEconomicoResponseDTO> buscarPorId(Integer id) {
        return estratoEconomicoRepository.findById(id)
                .map(estratoEconomicoMapper::toResponse);
    }

    @Override
    public EstratoEconomicoResponseDTO actualizar(Integer id, EstratoEconomicoRequestDTO request) {
        return estratoEconomicoRepository.findById(id).map(estratoEconomico -> {
            // Actualizamos los campos necesarios
            estratoEconomico.setNombre(request.nombre());

            return estratoEconomicoMapper.toResponse(estratoEconomicoRepository.save(estratoEconomico));
        }).orElseThrow(() -> new RuntimeException("No se encontró el estrato economico con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!estratoEconomicoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Estrato Economico no encontrado");
        }
        estratoEconomicoRepository.deleteById(id);
    }
}
