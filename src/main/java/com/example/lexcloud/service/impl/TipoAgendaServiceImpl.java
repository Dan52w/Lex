package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoAgendaMapper;
import com.example.lexcloud.dto.request.TipoAgentaResquestDTO;
import com.example.lexcloud.dto.response.TipoAgendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Agenda;
import com.example.lexcloud.repositorios.TipoAgenda_Repository;
import com.example.lexcloud.service.TipoAgendaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoAgendaServiceImpl implements TipoAgendaService {
    private final TipoAgenda_Repository tipoAgendaRepository;
    private final TipoAgendaMapper tipoAgendaMapper;

    public TipoAgendaServiceImpl(TipoAgenda_Repository tipoAgendaRepository, TipoAgendaMapper tipoAgendaMapper) {
        this.tipoAgendaRepository = tipoAgendaRepository;
        this.tipoAgendaMapper = tipoAgendaMapper;
    }

    @Override
    public TipoAgendaResponseDTO guardar(TipoAgentaResquestDTO request) {
        if (tipoAgendaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Agenda con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Agenda tipoAgenda = tipoAgendaMapper.toEntity(request);
        try {
            return tipoAgendaMapper.toResponse(tipoAgendaRepository.save(tipoAgenda));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Agenda con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoAgendaResponseDTO> listarTodo() {
        return tipoAgendaMapper.toResponseList(tipoAgendaRepository.findAll());
    }

    @Override
    public List<TipoAgendaResponseDTO> listarPorNombre(String nombre) {
        return tipoAgendaMapper.toResponseList(tipoAgendaRepository.findAllByNombre(nombre));
    }

    @Override
    public Optional<TipoAgendaResponseDTO> buscarPorNombre(String nombre) {
        return tipoAgendaRepository.findByNombre(nombre)
                .map(tipoAgendaMapper::toResponse);
    }

    @Override
    public Optional<TipoAgendaResponseDTO> buscarPorId(Integer id) {
        return tipoAgendaRepository.findById(id)
                .map(tipoAgendaMapper::toResponse);
    }

    @Override
    public TipoAgendaResponseDTO actualizar(Integer id, TipoAgentaResquestDTO request) {
        return tipoAgendaRepository.findById(id).map(odlTipoAgenda -> {
            if(tipoAgendaRepository.findByNombre(request.nombre()).isPresent()) {
                throw new IllegalArgumentException("La agenda con el nombre '" + request.nombre() + "' ya existe.");
            }
            odlTipoAgenda.setNombre(request.nombre());
            return tipoAgendaMapper.toResponse(tipoAgendaRepository.save(odlTipoAgenda));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo agenda con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if(!tipoAgendaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Tipo agenda no encontrada con ID: " + id);
        }
        tipoAgendaRepository.deleteById(id);
    }
}
