package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoAbogadoMapper;
import com.example.lexcloud.dto.request.TipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.TipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Abogado;
import com.example.lexcloud.repositorios.TipoAbogado_Repository;
import com.example.lexcloud.service.TipoAbogadoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoAbogadoServiceImpl implements TipoAbogadoService {
    private final TipoAbogado_Repository tipoAbogadoRepository;
    private final TipoAbogadoMapper tipoAbogadoMapper;

    public TipoAbogadoServiceImpl(TipoAbogado_Repository tipoAbogadoRepository, TipoAbogadoMapper tipoAbogadoMapper) {
        this.tipoAbogadoRepository = tipoAbogadoRepository;
        this.tipoAbogadoMapper = tipoAbogadoMapper;
    }

    @Override
    public TipoAbogadoResponseDTO guardar(TipoAbogadoRequestDTO request) {
        if(tipoAbogadoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalArgumentException("La especialización con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Abogado tipoAbogado = tipoAbogadoMapper.toEntity(request);
        return tipoAbogadoMapper.toResponse(tipoAbogadoRepository.save(tipoAbogado));
    }

    @Override
    public List<TipoAbogadoResponseDTO> listarTodo() {
        return tipoAbogadoMapper.toResponseList(tipoAbogadoRepository.findAll());
    }

    @Override
    public List<TipoAbogadoResponseDTO> listarPorNombre(String nombre) {
        return tipoAbogadoMapper.toResponseList(tipoAbogadoRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<TipoAbogadoResponseDTO> buscarPorNombre(String nombre) {
        return tipoAbogadoRepository.findByNombre(nombre)
                .map(tipoAbogadoMapper::toResponse);
    }

    @Override
    public Optional<TipoAbogadoResponseDTO> buscarPorId(Integer id) {
        return tipoAbogadoRepository.findById(id)
                .map(tipoAbogadoMapper::toResponse);
    }

    @Override
    public TipoAbogadoResponseDTO actualizar(Integer id, TipoAbogadoRequestDTO request) {
        return tipoAbogadoRepository.findById(id).map(oldTipoAbogado -> {
            if(tipoAbogadoRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("La especialización con el nombre '" + request.nombre() + "' ya existe.");
            }
            oldTipoAbogado.setNombre(request.nombre());
            return tipoAbogadoMapper.toResponse(tipoAbogadoRepository.save(oldTipoAbogado));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró la especialización con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!tipoAbogadoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: especialización no encontrada con ID: " + id);
        }
        tipoAbogadoRepository.deleteById(id);
    }
}
