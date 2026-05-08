package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoCasoMapper;
import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.service.VinculoCasoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VinculoCasoServiceImpl implements VinculoCasoService {

    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Persona_Repository personaRepository; // Para Persona de la firma
    private final Cliente_Repository clienteRepository; // Asumiendo que tienes este repo
    private final Caso_Repository casoRepository;       // Asumiendo que tienes este repo
    private final Vinculo_Repository vinculoRepository;
    private final VinculoCasoMapper vinculoCasoMapper;

    public VinculoCasoServiceImpl(VinculoCaso_Repository vinculoCasoRepository,
                                  Persona_Repository personaRepository,
                                  Cliente_Repository clienteRepository,
                                  Caso_Repository casoRepository,
                                  Vinculo_Repository vinculoRepository,
                                  VinculoCasoMapper vinculoCasoMapper) {
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.casoRepository = casoRepository;
        this.vinculoRepository = vinculoRepository;
        this.vinculoCasoMapper = vinculoCasoMapper;
    }

    @Override
    public VinculoCasoResponseDTO vincular(VinculoCasoRequestDTO request) {
        // 1. Validaciones de existencia
        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Persona de la firma no encontrada"));

        // Aquí podrías validar si la persona está activa
        if (!persona.getActivo()) {
            throw new IllegalArgumentException("No se puede vincular a una persona inactiva");
        }

        Vinculo_Caso vinculoCaso = vinculoCasoMapper.toEntity(request);

        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(personaRepository.findById(request.idCliente())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado")));
        vinculoCaso.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado")));
        vinculoCaso.setVinculo(vinculoRepository.findById(request.idVinculo())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de vínculo no encontrado")));

        return vinculoCasoMapper.toResponse(vinculoCasoRepository.save(vinculoCaso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso) {
        return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findPersonalAsignadoAlCaso(idCaso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona) {
        return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findByPersonaFirma_IdAndAutorizadoTrue(idPersona));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion() {
        return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findByAutorizadoFalse());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso) {
        return vinculoCasoRepository.findAbogadoPrincipalDelCaso(idCaso)
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso) {
        return vinculoCasoRepository.findByCasoId(idCaso)
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    public void autorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(idVinculoCaso)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo no encontrado"));
        vc.setAutorizado(true);
        vinculoCasoRepository.save(vc);
    }

    @Override
    public void eliminarVinculo(Integer id) {
        if (!vinculoCasoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: Vínculo no existente");
        }
        vinculoCasoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Integer idPersona, Integer idCaso) {
        return vinculoCasoRepository.existsByPersonaFirmaAndCasoIdAndAutorizadoTrue(idPersona, idCaso);
    }
}