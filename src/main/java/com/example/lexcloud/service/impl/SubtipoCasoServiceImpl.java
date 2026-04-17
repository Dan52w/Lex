package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.SubtipoCasoMapper;
import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;
import com.example.lexcloud.entidades.Subtipo_Caso;
import com.example.lexcloud.entidades.Tipo_Caso;
import com.example.lexcloud.repositorios.SubtipoCaso_Repository;
import com.example.lexcloud.repositorios.TipoCaso_Repository; // Necesitarás este repo
import com.example.lexcloud.service.SubtipoCasoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubtipoCasoServiceImpl implements SubtipoCasoService {

    private final SubtipoCaso_Repository subtipoRepository;
    private final TipoCaso_Repository tipoRepository;
    private final SubtipoCasoMapper subtipoCasoMapper;

    public SubtipoCasoServiceImpl(SubtipoCaso_Repository subtipoRepository, TipoCaso_Repository tipoRepository, SubtipoCasoMapper subtipoCasoMapper) {
        this.subtipoRepository = subtipoRepository;
        this.tipoRepository = tipoRepository;
        this.subtipoCasoMapper = subtipoCasoMapper;
    }

    @Override
    @Transactional
    public SubtipoCasoResponseDTO guardar(SubtipoCasoRequestDTO request) {
        // Validar si ya existe el nombre en este tipo de caso
        subtipoRepository.findByNombre(request.nombre()).ifPresent(s -> {
            throw new IllegalArgumentException("Ya existe un subtipo con este nombre");
        });

        Tipo_Caso tipo = tipoRepository.findById(request.tipoCaso())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de Caso no encontrado"));

        Subtipo_Caso subtipo = new Subtipo_Caso();
        subtipo.setNombre(request.nombre());
        subtipo.setTipoCaso(tipo);

        return subtipoCasoMapper.toResponse(subtipoRepository.save(subtipo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtipoCasoResponseDTO> listarTodo() {
        List<Subtipo_Caso> subtipoCasos = subtipoRepository.findAll();
        return subtipoCasoMapper.toResponseList(subtipoCasos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtipoCasoResponseDTO> listarPorTipo(Integer idTipoCaso) {
        List<Subtipo_Caso> subtipoCasos = subtipoRepository.findByTipoCasoId(idTipoCaso);

        return subtipoCasoMapper.toResponseList(subtipoCasos);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubtipoCasoResponseDTO> buscarPorId(Integer id) {
        return subtipoRepository.findById(id).map(subtipoCasoMapper::toResponse);
    }

    @Override
    @Transactional
    public SubtipoCasoResponseDTO actualizar(Integer id, SubtipoCasoRequestDTO request) {
        return subtipoRepository.findById(id).map(oldSubtipo -> {
            Subtipo_Caso subtipo = subtipoRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Subtipo no encontrado"));

            Tipo_Caso tipo = tipoRepository.findById(request.tipoCaso())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo de Caso no encontrado"));

            subtipo.setNombre(request.nombre());
            subtipo.setTipoCaso(tipo);

            return subtipoCasoMapper.toResponse(subtipoRepository.save(subtipo));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró la sub tipo caso con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!subtipoRepository.existsById(id)) throw new EntityNotFoundException("ID no encontrado");
        subtipoRepository.deleteById(id);
    }
}