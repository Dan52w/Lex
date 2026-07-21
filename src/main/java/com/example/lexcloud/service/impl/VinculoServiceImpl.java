package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoMapper;
import com.example.lexcloud.dto.request.VinculoRequestDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;
import com.example.lexcloud.entidades.Vinculo;
import com.example.lexcloud.repositorios.Vinculo_Repository;
import com.example.lexcloud.service.VinculoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VinculoServiceImpl implements VinculoService {
    private final Vinculo_Repository vinculoRepository;
    private final VinculoMapper vinculoMapper;

    public VinculoServiceImpl(Vinculo_Repository vinculoRepository, VinculoMapper vinculoMapper) {
        this.vinculoRepository = vinculoRepository;
        this.vinculoMapper = vinculoMapper;
    }

    @Override
    public VinculoResponseDTO guardar(VinculoRequestDTO request) {
        if (vinculoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Vinculo con el nombre '" + request.nombre() + "' ya existe.");
        }

        Vinculo vinculo = vinculoMapper.toEntity(request);
        try {
            return vinculoMapper.toResponse(vinculoRepository.save(vinculo));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Vinculo con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<VinculoResponseDTO> listarTodo() {
        return vinculoMapper.toResponseList(vinculoRepository.findAll());
    }

    @Override
    public List<VinculoResponseDTO> listarPorNombre(String nombre) {
        return vinculoMapper.toResponseList(vinculoRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<VinculoResponseDTO> buscarPorNombre(String nombre) {
        return vinculoRepository.findByNombre(nombre)
                .map(vinculoMapper::toResponse);
    }

    @Override
    public Optional<VinculoResponseDTO> buscarPorId(Integer id) {
        return vinculoRepository.findById(id)
                .map(vinculoMapper::toResponse);
    }

    @Override
    public VinculoResponseDTO actualizar(Integer id, VinculoRequestDTO request) {
        return vinculoRepository.findById(id).map(oldTipoCaso -> {
            if(vinculoRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("El vinculo con el nombre '" + request.nombre() + "' ya existe.");
            }
            oldTipoCaso.setNombre(request.nombre());
            return vinculoMapper.toResponse(vinculoRepository.save(oldTipoCaso));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el vinculo con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!vinculoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Vinculo no encontrada con ID: " + id);
        }
        vinculoRepository.deleteById(id);
    }
}
