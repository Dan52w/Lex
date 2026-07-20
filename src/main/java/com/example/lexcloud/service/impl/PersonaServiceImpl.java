package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.PersonaMapper;
import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
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

    public PersonaServiceImpl(Persona_Repository personaRepository, Rol_Repository rolRepository, PersonaMapper personaMapper) {
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.personaMapper = personaMapper;
    }

    @Override
    @Transactional
    public PersonaResponseDTO guardar(PersonaRequestDTO request) {
        // 1. Validar unicidad de la CC
        if (personaRepository.findByCc(request.cc()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una persona registrada con la cédula: " + request.cc());
        }

        Persona persona = personaMapper.toEntity(request);

        persona.setRol(rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

        // Aseguramos que por defecto esté activo al crear
        persona.setActivo(true);

        return personaMapper.toResponse(personaRepository.save(persona));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorCc(String cc) {
        return personaRepository.findByCc(cc)
                .map(personaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorIdActivo(Integer id) {
        // Usamos el método que creamos anteriormente en el repositorio
        return personaRepository.findByIdAndActivoTrue(id)
                .map(personaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorId(Integer id) {
        return personaRepository.findById(id)
                .map(personaMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonaResponseDTO actualizar(Integer id, PersonaRequestDTO request) {
        return personaRepository.findById(id).map(oldPersona -> {
            oldPersona.setNombre(request.nombre());
            oldPersona.setApellido(request.apellido());
            //oldPersona.setCc(request.cc()); La cedula no deberia ser editable
            oldPersona.setTelefono(request.telefono());
            oldPersona.setFechaNacimiento(request.fechaNacimiento());
            oldPersona.setActivo(true);

            oldPersona.setRol(rolRepository.findById(request.idRol())
                    .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

            return personaMapper.toResponse(personaRepository.save(oldPersona));
        }).orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con ID: " + id));
    }

    @Override
    public boolean existeCc(String cc) {
        return personaRepository.existsByCc(cc);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede desactivar: Persona no encontrada"));

        persona.setActivo(false);
        personaRepository.save(persona);
    }
}