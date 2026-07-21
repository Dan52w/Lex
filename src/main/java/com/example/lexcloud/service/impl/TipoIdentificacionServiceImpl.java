package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.TipoIdentificacionMapper;
import com.example.lexcloud.dto.request.TipoIdentificacionRequestDTO;
import com.example.lexcloud.dto.response.TipoIdenficacionResponseDTO;
import com.example.lexcloud.entidades.Tipo_Identificacion;
import com.example.lexcloud.repositorios.TipoIdentificacion_Repository;
import com.example.lexcloud.service.TipoIdentificacionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoIdentificacionServiceImpl implements TipoIdentificacionService {
    private final TipoIdentificacion_Repository tipoIdentificacionRepository;
    private final TipoIdentificacionMapper tipoIdentificacionMapper;

    public TipoIdentificacionServiceImpl(TipoIdentificacion_Repository tipoIdentificacionRepository,
                                         TipoIdentificacionMapper tipoIdentificacionMapper) {
        this.tipoIdentificacionRepository = tipoIdentificacionRepository;
        this.tipoIdentificacionMapper = tipoIdentificacionMapper;
    }

    @Override
    public TipoIdenficacionResponseDTO guardar(TipoIdentificacionRequestDTO request) {
        if (tipoIdentificacionRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Tipo Identificacion con el nombre '" + request.nombre() + "' ya existe.");
        }

        Tipo_Identificacion tipoIdentificacion = tipoIdentificacionMapper.toEntity(request);
        try {
            return tipoIdentificacionMapper.toResponse(tipoIdentificacionRepository.save(tipoIdentificacion));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Tipo Identificacion con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    public List<TipoIdenficacionResponseDTO> listarPorNombre(String nombre) {
        return tipoIdentificacionMapper.toResponseList(tipoIdentificacionRepository.findAllByNombre(nombre));
    }

    @Override
    public List<TipoIdenficacionResponseDTO> listarTodos() {
        return tipoIdentificacionMapper.toResponseList(tipoIdentificacionRepository.findAll());
    }

    @Override
    public Optional<TipoIdenficacionResponseDTO> buscarPorNombre(String nombre) {
        return tipoIdentificacionRepository.findByNombre(nombre)
                .map(tipoIdentificacionMapper::toResponse);
    }

    @Override
    public Optional<TipoIdenficacionResponseDTO> buscarPorId(Integer id) {
        return tipoIdentificacionRepository.findById(id)
                .map(tipoIdentificacionMapper::toResponse);
    }

    @Override
    public TipoIdenficacionResponseDTO actualizar(Integer id, TipoIdentificacionRequestDTO request) {
        return tipoIdentificacionRepository.findById(id).map(tipoIdentifiacion -> {
            // Actualizamos los campos necesarios
            tipoIdentifiacion.setNombre(request.nombre());

            return tipoIdentificacionMapper.toResponse(tipoIdentificacionRepository.save(tipoIdentifiacion));
        }).orElseThrow(() -> new RuntimeException("No se encontró el tipo identificacion con ID: " + id));
    }

    @Override
    public void eliminar(Integer id) {
        if (!tipoIdentificacionRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Tipo identificacion no encontrado");
        }
        tipoIdentificacionRepository.deleteById(id);
    }
}
