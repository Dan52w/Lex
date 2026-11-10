package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoCasoMapper;
import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.VinculoCasoService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class VinculoCasoServiceImpl implements VinculoCasoService {

    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Persona_Repository personaRepository;
    private final Cliente_Repository clienteRepository;
    private final Caso_Repository casoRepository;
    private final Vinculo_Repository vinculoRepository;
    private final Abogado_Repository abogadoRepository;
    private final VinculoCasoMapper vinculoCasoMapper;
    private final ContextoSeguridad contextoSeguridad;

    public VinculoCasoServiceImpl(VinculoCaso_Repository vinculoCasoRepository,
                                  Persona_Repository personaRepository,
                                  Cliente_Repository clienteRepository,
                                  Caso_Repository casoRepository,
                                  Vinculo_Repository vinculoRepository,
                                  Abogado_Repository abogadoRepository,
                                  VinculoCasoMapper vinculoCasoMapper,
                                  ContextoSeguridad contextoSeguridad) {
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.casoRepository = casoRepository;
        this.vinculoRepository = vinculoRepository;
        this.abogadoRepository = abogadoRepository;
        this.vinculoCasoMapper = vinculoCasoMapper;
        this.contextoSeguridad = contextoSeguridad;
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

        // 3b. Quien vincula debe tener acceso al caso (misma firma o vínculo autorizado).
        contextoSeguridad.verificarAccesoACaso(caso.getId());

        // 4. CONTROL DE SEGURIDAD CRÍTICO (Multi-tenancy cruzado):
        // Validamos que el cliente y el caso pertenezcan al mismo bufete (Firma)
        if (caso.getFirma() != null && !cliente.getFirma().getId().equals(caso.getFirma().getId())) {
            throw new SecurityException("Inconsistencia de seguridad: El cliente y el caso no pertenecen a la misma firma.");
        }

        // 4b. La persona vinculada también debe ser de la firma del caso, cuando
        // su firma se pueda resolver (tiene registro de abogado). El modelo no
        // guarda la firma en Persona, así que una persona sin registro de abogado
        // no se puede verificar por este medio (riesgo residual documentado).
        abogadoRepository.findByPersonaId(persona.getId())
                .ifPresent(abogado -> {
                    if (caso.getFirma() != null
                            && !abogado.getFirma().getId().equals(caso.getFirma().getId())) {
                        throw new SecurityException(
                                "La persona pertenece a otra firma y no se puede vincular a este caso.");
                    }
                });

        // 4c. Sin duplicados: la BD tiene UNIQUE(persona, cliente, caso).
        if (vinculoCasoRepository.existsByPersonaFirma_IdAndCliente_IdAndCaso_Id(
                persona.getId(), cliente.getId(), caso.getId())) {
            throw new IllegalStateException("Ya existe un vínculo entre esta persona, el cliente y el caso.");
        }

        // 4d. Integridad (SVC-9): solo puede haber un vínculo 'Principal' por caso.
        if ("Principal".equalsIgnoreCase(vinculo.getNombre())
                && !vinculoCasoRepository.findPrincipalesDelCaso(caso.getId()).isEmpty()) {
            throw new IllegalStateException("El caso ya tiene un abogado principal asignado.");
        }

        // 5. Construir la entidad de asociación usando el Mapper
        Vinculo_Caso vinculoCaso = vinculoCasoMapper.toEntity(request);

        // Seteamos manualmente las entidades que buscamos en la base de datos
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setCaso(caso);
        vinculoCaso.setVinculo(vinculo);

        // 6. AUTORIZACIÓN EXPLÍCITA (W3): un vínculo nuevo nace SIN autorizar.
        // El campo 'autorizado' del DTO lo envía el cliente y se ignora a
        // propósito: si se respetara, cualquiera podría auto-autorizarse.
        // La autorización solo ocurre por 'autorizarAcceso', con el actor
        // verificado contra la firma del caso. El vínculo 'Principal' que crea
        // CasoService.guardar al radicar un caso es la única excepción: ahí el
        // propio creador autoriza su vínculo al crear el caso.
        vinculoCaso.setAutorizado(false);

        return vinculoCasoMapper.toResponse(vinculoCasoRepository.save(vinculoCaso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso) {
        return listarPersonalAsignado(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso, int page, int size) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        return vinculoCasoMapper.toResponseList(
                vinculoCasoRepository.findPersonalAsignadoAlCaso(idCaso, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona) {
        return listarMisCasos(idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        List<Vinculo_Caso> vinculos = vinculoCasoRepository
                .findByPersonaFirma_IdAndAutorizadoTrue(idPersona, Paginacion.crear(page, size));
        if (contextoSeguridad.esAdmin()
                && !idPersona.equals(contextoSeguridad.idPersonaAutenticada())) {
            // Un administrador consulta los casos de otra persona: solo los de su firma.
            Integer firmaAdmin = contextoSeguridad.idFirmaAutenticada().orElse(null);
            vinculos = vinculos.stream()
                    .filter(vc -> firmaAdmin != null
                            && vc.getCaso().getFirma() != null
                            && firmaAdmin.equals(vc.getCaso().getFirma().getId()))
                    .collect(Collectors.toList());
        }
        return vinculoCasoMapper.toResponseList(vinculos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion() {
        return listarPendientesAutorizacion(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion(int page, int size) {
        // SVC-3: la variante global (todas las firmas) es solo para administradores;
        // el resto ve únicamente los pendientes de su propia firma.
        if (contextoSeguridad.esAdmin()) {
            return vinculoCasoMapper.toResponseList(
                    vinculoCasoRepository.findByAutorizadoFalse(Paginacion.crear(page, size)));
        }
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return vinculoCasoMapper.toResponseList(
                vinculoCasoRepository.findPendientesAutorizacionPorFirma(firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        // Lista en vez de Optional: si por datos viejos hubiera dos 'Principal',
        // el Optional original rompía con 500; aquí se toma el primero.
        return vinculoCasoRepository.findPrincipalesDelCaso(idCaso).stream()
                .findFirst()
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        // SVC-1: un caso tiene N vínculos; el antiguo findByCaso_Id (Optional)
        // rompía con 500. Se toma el primero, con el 'Principal' antes que los demás.
        return vinculoCasoRepository.findVinculosDelCaso(idCaso).stream()
                .findFirst()
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    public void autorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(idVinculoCaso)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de caso no encontrado con ID: " + idVinculoCaso));
        // Solo alguien con acceso al caso (misma firma o vínculo autorizado) puede autorizar.
        contextoSeguridad.verificarAccesoACaso(vc.getCaso().getId());
        vc.setAutorizado(true);
        vinculoCasoRepository.save(vc);
    }

    @Override
    public void eliminarVinculo(Integer id) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar: El vínculo de caso con ID " + id + " no existe."));
        contextoSeguridad.verificarAccesoACaso(vc.getCaso().getId());
        vinculoCasoRepository.delete(vc);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Integer idPersona, Integer idCaso) {
        // La consulta es sobre el propio acceso, salvo administradores.
        contextoSeguridad.verificarMismaPersona(idPersona);
        return vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(idPersona, idCaso);
    }
}
