package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.PersonaMapper;
import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.service.PersonaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonaServiceImpl implements PersonaService {

    /** Cédula de la persona del sistema (sembrada por la migración V1). */
    static final String CC_PERSONA_SISTEMA = "0000000000";

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
            throw new IllegalStateException("Ya existe una persona registrada con la cédula: " + request.cc());
        }

        Persona persona = personaMapper.toEntity(request);

        persona.setRol(rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

        // Aseguramos que por defecto esté activo al crear
        persona.setActivo(true);

        try {
            return personaMapper.toResponse(personaRepository.save(persona));
        } catch (DataIntegrityViolationException ex) {
            // Defensa en profundidad (carrera entre dos peticiones con la misma CC).
            throw new IllegalStateException("Ya existe una persona registrada con la cédula: " + request.cc(), ex);
        }
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
            vetarPersonaSistema(oldPersona);
            oldPersona.setNombre(request.nombre());
            oldPersona.setApellido(request.apellido());
            //oldPersona.setCc(request.cc()); La cedula no deberia ser editable
            oldPersona.setTelefono(request.telefono());
            oldPersona.setFechaNacimiento(request.fechaNacimiento());
            // El estado 'activo' NO se toca aquí: antes se forzaba a true en
            // cada actualización, lo que reactivaba en silencio a una persona
            // desactivada. La (des)activación es solo por desactivar().

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

        vetarPersonaSistema(persona);
        persona.setActivo(false);
        personaRepository.save(persona);
    }

    /**
     * S1: la persona del sistema no se puede desactivar ni modificar.
     *
     * La entidad Persona tiene {@code @SQLDelete} (UPDATE activo=false en vez
     * de DELETE): si algún día se llamara a {@code personaRepository.delete()},
     * Hibernate emitiría el UPDATE y el trigger V3 de borrado de personas
     * (BEFORE DELETE, que registra el cambio y protege la fila) no se
     * dispararía nunca — la baja quedaría sin constancia. Por eso el único
     * camino de baja es este método, con save() explícito, y el borrado físico
     * de personas no existe en ningún servicio (verificado por grep: ningún
     * código llama a personaRepository.delete). La decisión de quitar o no el
     * {@code @SQLDelete} queda pendiente: no se puede probar aquí cómo
     * reacciona Hibernate si el trigger devuelve 0 filas afectadas.
     */
    private void vetarPersonaSistema(Persona persona) {
        if (CC_PERSONA_SISTEMA.equals(persona.getCc())) {
            throw new SecurityException("La persona del sistema no se puede modificar ni desactivar.");
        }
    }
}