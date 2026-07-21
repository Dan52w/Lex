package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoClienteMapper;
import com.example.lexcloud.dto.request.TipoClienteRequestDTO;
import com.example.lexcloud.dto.response.TipoClienteResponseDTO;
import com.example.lexcloud.entidades.Tipo_Cliente;
import com.example.lexcloud.repositorios.TipoCliente_Repository;
import com.example.lexcloud.service.TipoClienteService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoClienteServiceImpl implements TipoClienteService {
    private final TipoCliente_Repository tipoClienteRepository;
    private final TipoClienteMapper tipoClienteMapper;

    public TipoClienteServiceImpl(TipoCliente_Repository tipoClienteRepository, TipoClienteMapper tipoClienteMapper) {
        this.tipoClienteRepository = tipoClienteRepository;
        this.tipoClienteMapper = tipoClienteMapper;
    }

    @Override
    public TipoClienteResponseDTO guardar(TipoClienteRequestDTO request) {
        if (tipoClienteRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Cliente con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Cliente tipoCliente = tipoClienteMapper.toEntity(request);
        try {
            return tipoClienteMapper.toResponse(tipoClienteRepository.save(tipoCliente));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Cliente con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoClienteResponseDTO> listarTodo() {
        return tipoClienteMapper.toResponseList(tipoClienteRepository.findAll());
    }

    @Override
    public List<TipoClienteResponseDTO> listarPorNombre(String nombre) {
        return tipoClienteMapper.toResponseList(tipoClienteRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<TipoClienteResponseDTO> buscarPorNombre(String nombre) {
        return tipoClienteRepository.findByNombre(nombre)
                .map(tipoClienteMapper::toResponse);
    }

    @Override
    public Optional<TipoClienteResponseDTO> buscarPorId(Integer id) {
        return tipoClienteRepository.findById(id)
                .map(tipoClienteMapper::toResponse);
    }

    @Override
    public TipoClienteResponseDTO actualizar(Integer id, TipoClienteRequestDTO request) {
        return tipoClienteRepository.findById(id).map(oldTipoCaso -> {
            if(tipoClienteRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("El tipo cliente con el nombre '" + request.nombre() + "' ya existe.");
            }
            oldTipoCaso.setNombre(request.nombre());
            return tipoClienteMapper.toResponse(tipoClienteRepository.save(oldTipoCaso));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo cliente con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!tipoClienteRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Tipo cliente no encontrada con ID: " + id);
        }
        tipoClienteRepository.deleteById(id);
    }
}
