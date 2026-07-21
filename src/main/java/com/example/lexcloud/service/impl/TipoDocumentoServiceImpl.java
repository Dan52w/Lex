package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoDocumentoMapper;
import com.example.lexcloud.dto.request.TipoDocumentoRequestDTO;
import com.example.lexcloud.dto.response.TipoDocumentoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Documento;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.service.TipoDocumentoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoDocumentoServiceImpl implements TipoDocumentoService {
    private final TipoDocumento_Repository tipoDocumentoRepository;
    private final TipoDocumentoMapper tipoDocumentoMapper;

    public TipoDocumentoServiceImpl(TipoDocumento_Repository tipoDocumentoRepository, TipoDocumentoMapper tipoDocumentoMapper) {
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.tipoDocumentoMapper = tipoDocumentoMapper;
    }

    @Override
    public TipoDocumentoResponseDTO guardar(TipoDocumentoRequestDTO request) {
        if (tipoDocumentoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Documento con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Documento tipoDocumento = tipoDocumentoMapper.toEntity(request);
        try {
            return tipoDocumentoMapper.toResponse(tipoDocumentoRepository.save(tipoDocumento));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Documento con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoDocumentoResponseDTO> listarTodo() {
        return tipoDocumentoMapper.toResponseList(tipoDocumentoRepository.findAll());
    }

    @Override
    public List<TipoDocumentoResponseDTO> listarPorNombre(String nombre) {
        return tipoDocumentoMapper.toResponseList(tipoDocumentoRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<TipoDocumentoResponseDTO> buscarPorNombre(String nombre) {
        return tipoDocumentoRepository.findByNombre(nombre)
                .map(tipoDocumentoMapper::toResponse);
    }

    @Override
    public Optional<TipoDocumentoResponseDTO> buscarPorId(Integer id) {
        return tipoDocumentoRepository.findById(id)
                .map(tipoDocumentoMapper::toResponse);
    }

    @Override
    public TipoDocumentoResponseDTO actualizar(Integer id, TipoDocumentoRequestDTO request) {
        return tipoDocumentoRepository.findById(id).map(oldTipoCaso -> {
            if(tipoDocumentoRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("El tipo Documento con el nombre '" + request.nombre() + "' ya existe.");
            }
            oldTipoCaso.setNombre(request.nombre());
            return tipoDocumentoMapper.toResponse(tipoDocumentoRepository.save(oldTipoCaso));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo Documento con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!tipoDocumentoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Tipo Documento no encontrada con ID: " + id);
        }
        tipoDocumentoRepository.deleteById(id);
    }
}
