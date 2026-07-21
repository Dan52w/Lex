package com.example.lexcloud.service.impl;



import com.example.lexcloud.dto.mappers.TipoRelacionEquipoMapper;
import com.example.lexcloud.dto.request.TipoRelacionEquipoRequestDTO;
import com.example.lexcloud.dto.response.TipoRelacionEquipoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Relacion_Equipo;
import com.example.lexcloud.repositorios.TipoRelacionEquipo_Repository;
import com.example.lexcloud.service.TipoRelacionEquipoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoRelacionEquipoServiceImpl implements TipoRelacionEquipoService {
    private final TipoRelacionEquipo_Repository tipoRelacionEquipoRepository;
    private final TipoRelacionEquipoMapper tipoRelacionEquipoMapper;

    public TipoRelacionEquipoServiceImpl(TipoRelacionEquipo_Repository tipoRelacionEquipoRepository,
                                         TipoRelacionEquipoMapper tipoRelacionEquipoMapper) {
        this.tipoRelacionEquipoRepository = tipoRelacionEquipoRepository;
        this.tipoRelacionEquipoMapper = tipoRelacionEquipoMapper;
    }

    @Override
    public TipoRelacionEquipoResponseDTO guardar(TipoRelacionEquipoRequestDTO request) {
        if (tipoRelacionEquipoRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Relacion Equipo con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Relacion_Equipo tipoRelacionEquipo = tipoRelacionEquipoMapper.toEntity(request);
        try {
            return tipoRelacionEquipoMapper.toResponse(tipoRelacionEquipoRepository.save(tipoRelacionEquipo));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Relacion Equipo con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoRelacionEquipoResponseDTO> listarPorNombre(String nombre) {
        return tipoRelacionEquipoMapper.toResponseList(tipoRelacionEquipoRepository.findAllByNombre(nombre));
    }

    @Override
    public List<TipoRelacionEquipoResponseDTO> listarTodos() {
        return tipoRelacionEquipoMapper.toResponseList(tipoRelacionEquipoRepository.findAll());
    }

    @Override
    public Optional<TipoRelacionEquipoResponseDTO> buscarPorNombre(String nombre) {
        return tipoRelacionEquipoRepository.findByNombre(nombre)
                .map(tipoRelacionEquipoMapper::toResponse);
    }

    @Override
    public Optional<TipoRelacionEquipoResponseDTO> buscarPorId(Integer id) {
        return tipoRelacionEquipoRepository.findById(id)
                .map(tipoRelacionEquipoMapper::toResponse);
    }

    @Override
    public TipoRelacionEquipoResponseDTO actualizar(Integer id, TipoRelacionEquipoRequestDTO request) {
        return tipoRelacionEquipoRepository.findById(id).map(tipoRelacionEquipo -> {
            // Actualizamos los campos necesarios
            tipoRelacionEquipo.setNombre(request.nombre());

            return tipoRelacionEquipoMapper.toResponse(tipoRelacionEquipoRepository.save(tipoRelacionEquipo));
        }).orElseThrow(() -> new RuntimeException("No se encontró el tipo relacion equipo con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!tipoRelacionEquipoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Tipo relacion equipo no encontrado");
        }
        tipoRelacionEquipoRepository.deleteById(id);
    }
}
