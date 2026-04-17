package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoCasoMapper;
import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Caso;
import com.example.lexcloud.repositorios.TipoCaso_Repository;
import com.example.lexcloud.service.TipoCasoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoCasoServiceImpl implements TipoCasoService {
    private final TipoCaso_Repository tipoCasoRepository;
    private final TipoCasoMapper tipoCasoMapper;

    public TipoCasoServiceImpl(TipoCaso_Repository tipoCasoRepository, TipoCasoMapper tipoCasoMapper) {
        this.tipoCasoRepository = tipoCasoRepository;
        this.tipoCasoMapper = tipoCasoMapper;
    }

    @Override
    public TipoCasoResponseDTO guardar(TipoCasoRequestDTO request) {
        if(tipoCasoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalArgumentException("La Agenda con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Caso tipoCaso = tipoCasoMapper.toEntity(request);
        return tipoCasoMapper.toResponse(tipoCasoRepository.save(tipoCaso));
    }

    @Override
    public List<TipoCasoResponseDTO> listarTodo() {
        return tipoCasoMapper.toResponseList(tipoCasoRepository.findAll());
    }

    @Override
    public List<TipoCasoResponseDTO> listarPorNombre(String nombre) {
        return tipoCasoMapper.toResponseList(tipoCasoRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<TipoCasoResponseDTO> buscarPorNombre(String nombre) {
        return tipoCasoRepository.findByNombre(nombre)
                .map(tipoCasoMapper::toResponse);
    }

    @Override
    public Optional<TipoCasoResponseDTO> buscarPorId(Integer id) {
        return tipoCasoRepository.findById(id)
                .map(tipoCasoMapper::toResponse);
    }

    @Override
    public TipoCasoResponseDTO actualizar(Integer id, TipoCasoRequestDTO request) {
        return tipoCasoRepository.findById(id).map(oldTipoCaso -> {
            if(tipoCasoRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("El tipo caso con el nombre '" + request.nombre() + "' ya existe.");
            }
            oldTipoCaso.setNombre(request.nombre());
            return tipoCasoMapper.toResponse(tipoCasoRepository.save(oldTipoCaso));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo caso con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!tipoCasoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Tipo caso no encontrada con ID: " + id);
        }
        tipoCasoRepository.deleteById(id);
    }
}
