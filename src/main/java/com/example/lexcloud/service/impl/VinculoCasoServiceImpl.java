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
    private final Persona_Repository personaRepository;
    private final Cliente_Repository clienteRepository;
    private final Caso_Repository casoRepository;
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
        // 1. Validar existencia y estado activo de la Persona (Abogado/Staff)
        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Miembro de la firma no encontrado con ID: " + request.idPersona()));

        if (Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("No se puede vincular a un usuario inactivo de la firma.");
        }

        // 2. Validar existencia y estado activo del Cliente
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.idCliente()));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("No se puede vincular un caso a un cliente inactivo.");
        }

        // 3. Validar existencia del Caso y el Tipo de Vínculo
        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado con ID: " + request.idCaso()));

        Vinculo vinculo = vinculoRepository.findById(request.idVinculo())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de vínculo no encontrado con ID: " + request.idVinculo()));

        // 4. CONTROL DE SEGURIDAD CRÍTICO (Multi-tenancy cruzado):
        // Validamos que el cliente y el caso pertenezcan al mismo bufete (Firma)
        if (caso.getFirma() != null && !cliente.getFirma().getId().equals(caso.getFirma().getId())) {
            throw new SecurityException("Inconsistencia de seguridad: El cliente y el caso no pertenecen a la misma firma.");
        }

        // 5. Construir la entidad de asociación usando el Mapper
        Vinculo_Caso vinculoCaso = vinculoCasoMapper.toEntity(request);

        // Seteamos manualmente las entidades que buscamos en la base de datos
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setCaso(caso);
        vinculoCaso.setVinculo(vinculo);

        // Manejo del estado de autorización (por defecto false si viene nulo)
        vinculoCaso.setAutorizado(true);

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
        return vinculoCasoRepository.findByCaso_Id(idCaso)
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    public void autorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(idVinculoCaso)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de caso no encontrado con ID: " + idVinculoCaso));
        vc.setAutorizado(true);
        vinculoCasoRepository.save(vc);
    }

    @Override
    public void eliminarVinculo(Integer id) {
        if (!vinculoCasoRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar: El vínculo de caso con ID " + id + " no existe.");
        }
        vinculoCasoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Integer idPersona, Integer idCaso) {
        return vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(idPersona, idCaso);
    }
}