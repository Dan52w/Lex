package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.PersonaMapper;
import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.security.RefreshTokenService;
import com.example.lexcloud.service.PersonaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonaServiceImpl implements PersonaService {

    private final Persona_Repository personaRepository;
    private final Rol_Repository rolRepository;
    private final PersonaMapper personaMapper;
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;
    private final RefreshTokenService refreshTokenService;

    public PersonaServiceImpl(Persona_Repository personaRepository, Rol_Repository rolRepository, PersonaMapper personaMapper,
                              CurrentUser currentUser, AuditoriaService auditoriaService,
                              RefreshTokenService refreshTokenService) {
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.personaMapper = personaMapper;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
        this.refreshTokenService = refreshTokenService;
    }

    /** Lectura de personas: admin, uno mismo, o misma firma del token. */
    private void requireLecturaPersona(Persona persona) {
        if (currentUser.isAdmin() || currentUser.personaId().equals(persona.getId())) {
            return;
        }
        Integer firmaObjetivo = currentUser.firmaOfPersona(persona.getId());
        if (firmaObjetivo == null || !currentUser.canAccessFirma(firmaObjetivo)) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
    }

    @Override
    @Transactional
    public PersonaResponseDTO guardar(PersonaRequestDTO request) {
        // P0-B3: crear personas (y con ello, en el flujo de alta, cambiar
        // roles) es solo-admin; el controlador también lo exige.
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        // 1. Validar unicidad de la CC
        if (personaRepository.findByCc(request.cc()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + request.cc());
        }

        Persona persona = personaMapper.toEntity(request);

        persona.setRol(rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

        // Aseguramos que por defecto esté activo al crear
        persona.setActivo(true);

        PersonaResponseDTO creada = personaMapper.toResponse(personaRepository.save(persona));
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "PERSONA_CREADA", "persona", creada.id(), null);
        return creada;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorCc(String cc) {
        return personaRepository.findByCc(cc)
                .map(persona -> {
                    requireLecturaPersona(persona);
                    return personaMapper.toResponse(persona);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorIdActivo(Integer id) {
        return personaRepository.findByIdAndActivoTrue(id)
                .map(persona -> {
                    requireLecturaPersona(persona);
                    return personaMapper.toResponse(persona);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorId(Integer id) {
        return personaRepository.findById(id)
                .map(persona -> {
                    requireLecturaPersona(persona);
                    return personaMapper.toResponse(persona);
                });
    }

    @Override
    @Transactional
    public PersonaResponseDTO actualizar(Integer id, PersonaRequestDTO request) {
        // P0-B3: editar personas y cambiar roles es solo-admin, y editar
        // NUNCA reactiva una cuenta desactivada (se quitó setActivo(true)).
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        return personaRepository.findById(id).map(oldPersona -> {
            Integer rolAnterior = oldPersona.getRol() != null ? oldPersona.getRol().getId() : null;
            oldPersona.setNombre(request.nombre());
            oldPersona.setApellido(request.apellido());
            //oldPersona.setCc(request.cc()); La cedula no deberia ser editable
            oldPersona.setTelefono(request.telefono());
            oldPersona.setFechaNacimiento(request.fechaNacimiento());
            // P0-B3: NO tocar activo aquí. La reactivación es explícita
            // (ver reactivar()) y solo-admin, con registro de auditoría.

            oldPersona.setRol(rolRepository.findById(request.idRol())
                    .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

            PersonaResponseDTO actualizada = personaMapper.toResponse(personaRepository.save(oldPersona));
            if (rolAnterior == null || !rolAnterior.equals(request.idRol())) {
                auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                        "ROL_CAMBIADO", "persona", id, null);
            }
            return actualizada;
        }).orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con ID: " + id));
    }

    @Override
    public boolean existeCc(String cc) {
        return personaRepository.existsByCc(cc);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede desactivar: Persona no encontrada"));

        persona.setActivo(false);
        personaRepository.save(persona);
        // P0-B3/B6: al desactivar se revocan TODOS sus refresh tokens, así
        // que no puede renovar sesión; el access vigente expira en minutos.
        refreshTokenService.revokeAllForPersona(id);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "PERSONA_DESACTIVADA", "persona", id, null);
    }

    @Override
    @Transactional
    public void reactivar(Integer id) {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede reactivar: Persona no encontrada"));
        persona.setActivo(true);
        personaRepository.save(persona);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "PERSONA_REACTIVADA", "persona", id, null);
    }
}