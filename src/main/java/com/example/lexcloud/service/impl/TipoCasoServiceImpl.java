package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoCasoMapper;
import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Caso;
import com.example.lexcloud.repositorios.TipoCaso_Repository;
import com.example.lexcloud.service.TipoCasoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
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
        if (tipoCasoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Caso con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Caso tipoCaso = tipoCasoMapper.toEntity(request);
        try {
            return tipoCasoMapper.toResponse(tipoCasoRepository.save(tipoCaso));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Caso con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoCasoResponseDTO> listarTodo() {
        return tipoCasoMapper.toResponseList(tipoCasoRepository.findAll()).stream()
                .filter(dto -> dto.id() != 1)
                .toList();
    }

    @Override
    public List<TipoCasoResponseDTO> listarPorNombre(String nombre) {
        return tipoCasoMapper.toResponseList(tipoCasoRepository.findAllByNombre(nombre)).stream()
                .filter(dto -> dto.id() != 1)
                .toList();
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
            // W5 (cierre del veto de borrado): 'RAÍZ' tampoco se puede renombrar.
            if ("RAÍZ".equals(oldTipoCaso.getNombre()) && !oldTipoCaso.getNombre().equals(request.nombre())) {
                throw new IllegalStateException("El tipo de caso 'RAÍZ' es del sistema y no se puede renombrar.");
            }
            tipoCasoRepository.findByNombre(request.nombre())
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new IllegalStateException("El Tipo Caso con el nombre '" + request.nombre() + "' ya existe.");
                    });
            oldTipoCaso.setNombre(request.nombre());
            try {
                return tipoCasoMapper.toResponse(tipoCasoRepository.save(oldTipoCaso));
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalStateException("El Tipo Caso con el nombre '" + request.nombre() + "' ya existe.");
            }
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el tipo caso con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        Tipo_Caso tipoCaso = tipoCasoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar: Tipo caso no encontrada con ID: " + id));
        // W5: 'RAÍZ' es el tipo de caso raíz del sistema (seed V1); borrarlo rompería el caso maestro y los subtipos.
        if ("RAÍZ".equals(tipoCaso.getNombre())) {
            throw new IllegalStateException("No se puede eliminar el tipo de caso 'RAÍZ': es el tipo raíz del sistema.");
        }
        tipoCasoRepository.deleteById(id);
    }
}
