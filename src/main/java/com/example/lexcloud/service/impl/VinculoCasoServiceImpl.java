package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoCasoMapper;
import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
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
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;

    public VinculoCasoServiceImpl(VinculoCaso_Repository vinculoCasoRepository,
                                  Persona_Repository personaRepository,
                                  Cliente_Repository clienteRepository,
                                  Caso_Repository casoRepository,
                                  Vinculo_Repository vinculoRepository,
                                  VinculoCasoMapper vinculoCasoMapper,
                                  CurrentUser currentUser,
                                  AuditoriaService auditoriaService) {
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.casoRepository = casoRepository;
        this.vinculoRepository = vinculoRepository;
        this.vinculoCasoMapper = vinculoCasoMapper;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public VinculoCasoResponseDTO vincular(VinculoCasoRequestDTO request) {
        // P0-B2/B4: el solicitante es el del token. Debe pertenecer a la firma
        // del caso; la persona a vincular también debe pertenecer a esa firma.
        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Miembro de la firma no encontrado"));

        if (Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("No se puede vincular a un usuario inactivo de la firma.");
        }

        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("No se puede vincular un caso a un cliente inactivo.");
        }

        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));

        Vinculo vinculo = vinculoRepository.findById(request.idVinculo())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de vínculo no encontrado"));

        // Cliente y caso deben ser de la misma firma (control preexistente).
        if (caso.getFirma() != null && !cliente.getFirma().getId().equals(caso.getFirma().getId())) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        // El solicitante debe pertenecer a la firma del caso.
        currentUser.requireFirma(caso.getFirma() != null ? caso.getFirma().getId() : null);
        // La persona a vincular debe pertenecer a la misma firma.
        Integer firmaPersona = currentUser.firmaOfPersona(persona.getId());
        if (firmaPersona == null || !firmaPersona.equals(caso.getFirma().getId())) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }

        Vinculo_Caso vinculoCaso = vinculoCasoMapper.toEntity(request);
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setCaso(caso);
        vinculoCaso.setVinculo(vinculo);

        // P0-B4: NUNCA auto-autorizar al solicitante. El vínculo nace
        // pendiente (autorizado=false); solo un admin puede crearlo ya
        // autorizado, y la autorización posterior la otorga alguien distinto
        // del vinculado con acceso autorizado al caso (ver autorizarAcceso).
        // El campo autorizado del DTO se ignora deliberadamente.
        vinculoCaso.setAutorizado(currentUser.isAdmin());

        VinculoCasoResponseDTO creado = vinculoCasoMapper.toResponse(vinculoCasoRepository.save(vinculoCaso));
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "VINCULO_CREADO", "vinculo_caso", creado.id(), null);
        return creado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso) {
        currentUser.requireCaso(idCaso);
        return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findPersonalAsignadoAlCaso(idCaso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona) {
        // P0-B2: el idPersona de la URL se ignora; siempre los del token.
        return vinculoCasoMapper.toResponseList(
                vinculoCasoRepository.findByPersonaFirma_IdAndAutorizadoTrue(currentUser.personaId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion() {
        // P0-B4: pendientes SOLO de la firma del token (admin: todas).
        if (currentUser.isAdmin()) {
            return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findByAutorizadoFalse());
        }
        Integer firmaId = currentUser.requireFirmaId();
        return vinculoCasoMapper.toResponseList(vinculoCasoRepository.findPendientesPorFirma(firmaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso) {
        currentUser.requireCaso(idCaso);
        return vinculoCasoRepository.findAbogadoPrincipalDelCaso(idCaso)
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso) {
        currentUser.requireCaso(idCaso);
        return vinculoCasoRepository.findByCaso_Id(idCaso)
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    public void autorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(idVinculoCaso)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de caso no encontrado"));
        // P0-B4: quien autoriza debe ser admin, o miembro de la firma del caso
        // con acceso autorizado a ese caso, Y distinto de la persona vinculada
        // (nadie se autoriza a sí mismo).
        boolean puedeAutorizar = currentUser.isAdmin()
                || (vc.getCaso().getFirma() != null
                        && currentUser.canAccessFirma(vc.getCaso().getFirma().getId())
                        && currentUser.canAccessCaso(vc.getCaso().getId())
                        && !currentUser.personaId().equals(vc.getPersonaFirma().getId()));
        if (!puedeAutorizar) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        vc.setAutorizado(true);
        vinculoCasoRepository.save(vc);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "VINCULO_AUTORIZADO", "vinculo_caso", idVinculoCaso, null);
    }

    @Override
    public void eliminarVinculo(Integer id) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de caso no encontrado"));
        if (vc.getCaso().getFirma() != null) {
            currentUser.requireFirma(vc.getCaso().getFirma().getId());
        }
        vinculoCasoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Integer idPersona, Integer idCaso) {
        // P0-B2: los parámetros se ignoran; responde sobre el token.
        return currentUser.canAccessCaso(idCaso);
    }
}
