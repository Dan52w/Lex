package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.RolMapper;
import com.example.lexcloud.dto.request.RolRequestDTO;
import com.example.lexcloud.dto.response.RolResponseDTO;
import com.example.lexcloud.entidades.Rol;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.service.RolService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RolServiceImpl implements RolService {

    private final Rol_Repository rolRepository;
    private final RolMapper rolMapper;

    public RolServiceImpl(Rol_Repository rolRepository, RolMapper rolMapper) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    @Transactional
    public RolResponseDTO guardar(RolRequestDTO request) {
        if (rolRepository.findByNombre(request.nombre()).isPresent()) {
            throw new IllegalStateException("El Rol con el nombre '" + request.nombre() + "' ya existe.");
        }

        Rol rol = rolMapper.toEntity(request);
        try {
            return rolMapper.toResponse(rolRepository.save(rol));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El Rol con el nombre '" + request.nombre() + "' ya existe.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarTodo() {
        return rolRepository.findAll()
                .stream()
                .map(rolMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarPorNombre(String nombre) {
        return rolRepository.findAllByNombre(nombre)
                .stream()
                .map(rolMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RolResponseDTO> buscarPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
                .map(rolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RolResponseDTO> buscarPorId(Integer id) {
        return rolRepository.findById(id)
                .map(rolMapper::toResponse);
    }

    @Override
    @Transactional
    public RolResponseDTO actualizar(Integer id, RolRequestDTO request) {
        return rolRepository.findById(id).map(rolExistente -> {
            // W5 (cierre del veto de borrado): 'SISTEMA' tampoco se puede renombrar.
            if ("SISTEMA".equals(rolExistente.getNombre()) && !rolExistente.getNombre().equals(request.nombre())) {
                throw new IllegalStateException("El rol 'SISTEMA' es del sistema y no se puede renombrar.");
            }
            // Validar si el nuevo nombre ya lo tiene otro rol
            rolRepository.findByNombre(request.nombre())
                    .ifPresent(r -> {
                        if (!r.getId().equals(id)) {
                            throw new IllegalStateException("El Rol con el nombre '" + request.nombre() + "' ya existe.");
                        }
                    });

            rolExistente.setNombre(request.nombre());

            try {
                return rolMapper.toResponse(rolRepository.save(rolExistente));
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalStateException("El Rol con el nombre '" + request.nombre() + "' ya existe.");
            }
        }).orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar: Rol no encontrado con ID: " + id));
        // W5: 'SISTEMA' es el rol de la persona sistema (seed V1); borrarlo rompería el usuario sistema.
        if ("SISTEMA".equals(rol.getNombre())) {
            throw new IllegalStateException("No se puede eliminar el rol 'SISTEMA': es el rol de la persona sistema.");
        }
        // Nota: En sistemas con integridad referencial, esto fallará si hay personas usando el rol.
        // Podrías considerar una desactivación lógica si fuera necesario.
        rolRepository.deleteById(id);
    }
}