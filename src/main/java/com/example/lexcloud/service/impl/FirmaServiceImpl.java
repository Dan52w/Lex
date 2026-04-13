package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.FirmaMapper;
import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.service.FirmaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FirmaServiceImpl implements FirmaService {

    private final Firma_Repository firmaRepository;
    private final FirmaMapper firmaMapper;

    public FirmaServiceImpl(Firma_Repository firmaRepository, FirmaMapper firmaMapper) {
        this.firmaRepository = firmaRepository;
        this.firmaMapper = firmaMapper;
    }

    @Override
    public FirmaResponseDTO guardar(FirmaRequestDTO request) {
        // 1. Validar que el NIT no esté registrado previamente
        if (firmaRepository.findByNit(request.nit()).isPresent()) {
            throw new IllegalStateException("Ya existe una firma registrada con el NIT: " + request.nit());
        }

        Firma firma = firmaMapper.toEntity(request);

        return firmaMapper.toResponse(firmaRepository.save(firma));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FirmaResponseDTO> listarPorNombre(String nombre) {
        return firmaMapper.toResponseList(firmaRepository.findAllByNombre(nombre));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FirmaResponseDTO> buscarPorNit(String nit) {
        return firmaRepository.findByNit(nit)
                .map(firmaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FirmaResponseDTO> buscarPorId(Integer id) {
        return firmaRepository.findById(id)
                .map(firmaMapper::toResponse);
    }

    @Override
    public FirmaResponseDTO actualizar(Integer id, FirmaRequestDTO request) {
        return firmaRepository.findById(id).map(firma -> {
            // Actualizamos los datos básicos
            firma.setNombre(request.nombre());
            firma.setIntegrante(request.integrante());

            // Nota: Generalmente el NIT no se debería poder editar si ya hay documentos
            // guardados en carpetas con ese nombre. Si se edita, habría que renombrar carpetas.
            /*
            if (!firma.getNit().equals(request.nit())) {
                // Opcional: Lógica para renombrar carpeta física o lanzar error
                // firma.setNit(request.nit());
            }
            */

            return firmaMapper.toResponse(firmaRepository.save(firma));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontró la firma con ID: " + id));
    }
}