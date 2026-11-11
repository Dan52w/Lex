package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.VinculoCasoMapper;
import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.HistorialCambioService;
import com.example.lexcloud.service.VinculoCasoService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VinculoCasoServiceImpl implements VinculoCasoService {

    private static final Logger log = LoggerFactory.getLogger(VinculoCasoServiceImpl.class);

    /** Nombre del tipo de vínculo que no se puede asignar a un caso (M29). */
    private static final String NOMBRE_SIN_VINCULO = "SIN VÍNCULO";

    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Persona_Repository personaRepository;
    private final Cliente_Repository clienteRepository;
    private final Caso_Repository casoRepository;
    private final Vinculo_Repository vinculoRepository;
    private final Abogado_Repository abogadoRepository;
    private final VinculoCasoMapper vinculoCasoMapper;
    private final ContextoSeguridad contextoSeguridad;
    private final HistorialCambioService historialCambioService;

    public VinculoCasoServiceImpl(VinculoCaso_Repository vinculoCasoRepository,
                                  Persona_Repository personaRepository,
                                  Cliente_Repository clienteRepository,
                                  Caso_Repository casoRepository,
                                  Vinculo_Repository vinculoRepository,
                                  Abogado_Repository abogadoRepository,
                                  VinculoCasoMapper vinculoCasoMapper,
                                  ContextoSeguridad contextoSeguridad,
                                  HistorialCambioService historialCambioService) {
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.casoRepository = casoRepository;
        this.vinculoRepository = vinculoRepository;
        this.abogadoRepository = abogadoRepository;
        this.vinculoCasoMapper = vinculoCasoMapper;
        this.contextoSeguridad = contextoSeguridad;
        this.historialCambioService = historialCambioService;
    }

    @Override
    public VinculoCasoResponseDTO vincular(VinculoCasoRequestDTO request) {
        // A2: verificar acceso ANTES de cualquier findById, para no oracular
        // personas, clientes, casos ni vínculos que no se pueden ver.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

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

        // M29: el caso debe estar activo.
        if (!Boolean.TRUE.equals(caso.getActivo())) {
            throw new IllegalArgumentException("No se puede vincular a un caso inactivo.");
        }

        // M29: el tipo 'SIN VÍNCULO' no se puede asignar a un caso.
        if (vinculo.getNombre() != null
                && NOMBRE_SIN_VINCULO.equalsIgnoreCase(vinculo.getNombre().trim())) {
            throw new IllegalArgumentException("El tipo de vínculo 'SIN VÍNCULO' no se puede asignar a un caso.");
        }

        // 4. CONTROL DE SEGURIDAD CRÍTICO (Multi-tenancy cruzado):
        // Validamos que el cliente y el caso pertenezcan al mismo bufete (Firma)
        if (caso.getFirma() != null && !cliente.getFirma().getId().equals(caso.getFirma().getId())) {
            throw new SecurityException("Inconsistencia de seguridad: El cliente y el caso no pertenecen a la misma firma.");
        }

        // 4b. La persona vinculada también debe ser de la firma del caso, cuando
        // su firma se pueda resolver (tiene registro de abogado). Se mantiene
        // con .ifPresent y NO se convierte en denegación: el personal sin
        // registro de abogado recibe vínculos legítimamente (el portón es C7
        // —la autorización la decide un admin o el Principal autorizado— y el
        // vínculo nace PENDIENTE, sin acceso hasta que se autorice).
        abogadoRepository.findByPersonaId(persona.getId())
                .ifPresent(abogado -> {
                    if (caso.getFirma() != null
                            && !abogado.getFirma().getId().equals(caso.getFirma().getId())) {
                        throw new SecurityException(
                                "La persona pertenece a otra firma y no se puede vincular a este caso.");
                    }
                });

        // M18: un solo cliente por caso. Si el caso ya tiene vínculos con un
        // cliente distinto al del request, se rechaza (hacia adelante;
        // los datos viejos con varios clientes no se reescriben aquí).
        boolean otroCliente = vinculoCasoRepository
                .findVinculosDelCaso(caso.getId(), VinculoCaso_Repository.VINCULO_PRINCIPAL)
                .stream()
                .anyMatch(vc -> vc.getCliente() != null
                        && !cliente.getId().equals(vc.getCliente().getId()));
        if (otroCliente) {
            throw new IllegalStateException("El caso ya tiene vínculos con otro cliente.");
        }

        // 4c. Sin duplicados: la BD tiene UNIQUE(persona, cliente, caso).
        if (vinculoCasoRepository.existsByPersonaFirma_IdAndCliente_IdAndCaso_Id(
                persona.getId(), cliente.getId(), caso.getId())) {
            throw new IllegalStateException("Ya existe un vínculo entre esta persona, el cliente y el caso.");
        }

        // A8: solo puede haber un vínculo 'Principal' por caso. Se cuentan
        // TODOS los Principal del caso, en cualquier estado (un segundo
        // Principal pendiente también violaría la regla); el trigger V4 es
        // la red de seguridad si dos peticiones corren en paralelo.
        if (VinculoCaso_Repository.VINCULO_PRINCIPAL.equalsIgnoreCase(vinculo.getNombre())
                && vinculoCasoRepository.countPrincipalesDelCaso(
                        caso.getId(), VinculoCaso_Repository.VINCULO_PRINCIPAL) > 0) {
            throw new IllegalStateException("El caso ya tiene un abogado principal asignado.");
        }

        // 5. Construir la entidad de asociación usando el Mapper
        Vinculo_Caso vinculoCaso = vinculoCasoMapper.toEntity(request);

        // Seteamos manualmente las entidades que buscamos en la base de datos
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setCaso(caso);
        vinculoCaso.setVinculo(vinculo);

        // 6. AUTORIZACIÓN EXPLÍCITA (W3/M13): un vínculo nuevo nace PENDIENTE,
        // sin acceso. El estado nunca viene del DTO (el RequestDTO ya no trae
        // campo `autorizado`): si se respetara, cualquiera podría
        // auto-autorizarse. La autorización solo ocurre por 'autorizarAcceso',
        // con el actor verificado contra la firma del caso. El vínculo
        // 'Principal' que crea CasoService.guardar al radicar un caso es la
        // única excepción: ahí el propio creador autoriza su vínculo al crear
        // el caso.
        vinculoCaso.setEstado(EstadoVinculo.PENDIENTE);

        // M3: la violación del UNIQUE(persona, cliente, caso) en carrera se
        // traduce a IllegalStateException con mensaje claro, no al error
        // genérico de la BD.
        try {
            return vinculoCasoMapper.toResponse(vinculoCasoRepository.save(vinculoCaso));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Ya existe un vínculo entre esta persona, el cliente y el caso.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso) {
        return listarPersonalAsignado(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    /**
     * Solo devuelve vínculos AUTORIZADOS: los pendientes (y los rechazados)
     * no aparecen en el personal asignado del caso.
     */
    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso, int page, int size) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        return vinculoCasoMapper.toResponseList(
                vinculoCasoRepository.findPersonalAsignadoAlCaso(
                        idCaso, EstadoVinculo.AUTORIZADO, Paginacion.crear(page, size)));
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
        // M2: el filtrado por firma del admin se hace a nivel JPQL (variante
        // por firma del repositorio), no en memoria después de paginar.
        if (contextoSeguridad.esAdmin()
                && !idPersona.equals(contextoSeguridad.idPersonaAutenticada())) {
            // V5: el súper admin consulta los casos de otra persona sin
            // filtro de firma (global); el admin de firma solo los de su
            // firma. Sin firma resoluble no hay casos que mostrar (fail-closed).
            if (contextoSeguridad.esSuperAdmin()) {
                return vinculoCasoMapper.toResponseList(
                        vinculoCasoRepository.findByPersonaFirma_IdAndEstado(
                                idPersona, EstadoVinculo.AUTORIZADO, Paginacion.crear(page, size)));
            }
            // Un administrador consulta los casos de otra persona: solo los de su firma.
            // Sin firma resoluble no hay casos que mostrar (fail-closed).
            return contextoSeguridad.idFirmaAutenticada()
                    .map(firmaAdmin -> vinculoCasoMapper.toResponseList(
                            vinculoCasoRepository.findByPersonaFirma_IdAndEstadoAndCaso_Firma_Id(
                                    idPersona, EstadoVinculo.AUTORIZADO, firmaAdmin,
                                    Paginacion.crear(page, size))))
                    .orElse(List.of());
        }
        return vinculoCasoMapper.toResponseList(
                vinculoCasoRepository.findByPersonaFirma_IdAndEstado(
                        idPersona, EstadoVinculo.AUTORIZADO, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarMisCasosAutenticado(int page, int size) {
        // P0-B2: la persona es la del token; delega en el listado por
        // persona, que ya aplica verificarMismaPersona (pasa: es uno mismo).
        return listarMisCasos(contextoSeguridad.idPersonaAutenticada(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion() {
        return listarPendientesAutorizacion(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VinculoCasoResponseDTO> listarPendientesAutorizacion(int page, int size) {
        // C8: la rama global (pendientes de TODAS las firmas) desaparece.
        // Si la firma del usuario es resoluble, ve los pendientes de su firma.
        // M26: si NO es resoluble (personal sin registro de abogado), ve los
        // pendientes de los casos en los que tiene un vínculo AUTORIZADO;
        // no se lanza 403: ese perfil accede por vínculo autorizado.
        // V5: el súper admin es la única excepción a C8: ve los pendientes
        // de todas las firmas (variante global del repositorio).
        if (contextoSeguridad.esSuperAdmin()) {
            return vinculoCasoMapper.toResponseList(
                    vinculoCasoRepository.findPendientesAutorizacion(Paginacion.crear(page, size)));
        }
        return contextoSeguridad.idFirmaAutenticada()
                .map(firmaId -> vinculoCasoMapper.toResponseList(
                        vinculoCasoRepository.findPendientesAutorizacionPorFirma(
                                firmaId, Paginacion.crear(page, size))))
                .orElseGet(() -> vinculoCasoMapper.toResponseList(
                        vinculoCasoRepository.findPendientesPorPersona(
                                contextoSeguridad.idPersonaAutenticada(),
                                Paginacion.crear(page, size))));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        // M1: variante paginada (página 0, tamaño 1) en vez de cargar la lista
        // entera. El ORDER BY pone el 'Principal' primero, así que el primer
        // elemento es el principal si lo hay; se conserva el filtro de antes
        // (Principal y AUTORIZADO) sobre ese único elemento.
        return vinculoCasoRepository
                .findVinculosDelCaso(idCaso, VinculoCaso_Repository.VINCULO_PRINCIPAL,
                        Paginacion.crear(0, 1))
                .stream()
                .filter(vc -> vc.getEstado() == EstadoVinculo.AUTORIZADO
                        && vc.getVinculo() != null
                        && VinculoCaso_Repository.VINCULO_PRINCIPAL
                                .equalsIgnoreCase(vc.getVinculo().getNombre()))
                .findFirst()
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        // M1: variante paginada (página 0, tamaño 1) en vez de cargar la lista
        // entera; se devuelve el cliente del primer vínculo (Principal
        // primero). vincular() ya impone un-cliente-por-caso hacia adelante
        // (M18), así que en los casos nuevos ese primer vínculo define el
        // cliente del caso.
        return vinculoCasoRepository
                .findVinculosDelCaso(idCaso, VinculoCaso_Repository.VINCULO_PRINCIPAL,
                        Paginacion.crear(0, 1))
                .stream()
                .findFirst()
                .map(vinculoCasoMapper::toResponse);
    }

    @Override
    public void autorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = cargarParaGobierno(idVinculoCaso);
        if (vc.getEstado() == EstadoVinculo.AUTORIZADO) {
            throw new IllegalStateException("El vínculo ya está autorizado.");
        }
        vc.setEstado(EstadoVinculo.AUTORIZADO);
        vinculoCasoRepository.save(vc);
        auditar(vc.getCaso().getId(),
                "Vínculo autorizado: vínculo " + vc.getId()
                        + " (persona " + vc.getPersonaFirma().getId()
                        + ", caso " + vc.getCaso().getId() + ").");
    }

    @Override
    public void desautorizarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = cargarParaGobierno(idVinculoCaso);
        // Solo desde AUTORIZADO: un vínculo PENDIENTE o RECHAZADO no tiene
        // nada que desautorizar.
        if (vc.getEstado() != EstadoVinculo.AUTORIZADO) {
            throw new IllegalStateException("Solo un vínculo autorizado puede desautorizarse.");
        }
        vc.setEstado(EstadoVinculo.PENDIENTE);
        vinculoCasoRepository.save(vc);
        auditar(vc.getCaso().getId(),
                "Vínculo desautorizado: vínculo " + vc.getId()
                        + " (persona " + vc.getPersonaFirma().getId()
                        + ", caso " + vc.getCaso().getId() + ").");
    }

    @Override
    public void rechazarAcceso(Integer idVinculoCaso) {
        Vinculo_Caso vc = cargarParaGobierno(idVinculoCaso);
        // Solo desde PENDIENTE: desde AUTORIZADO hay que desautorizar primero.
        if (vc.getEstado() == EstadoVinculo.AUTORIZADO) {
            throw new IllegalStateException(
                    "El vínculo está autorizado; desautorícelo antes de rechazarlo.");
        }
        if (vc.getEstado() == EstadoVinculo.RECHAZADO) {
            throw new IllegalStateException("El vínculo ya está rechazado.");
        }
        vc.setEstado(EstadoVinculo.RECHAZADO);
        vinculoCasoRepository.save(vc);
        auditar(vc.getCaso().getId(),
                "Vínculo rechazado: vínculo " + vc.getId()
                        + " (persona " + vc.getPersonaFirma().getId()
                        + ", caso " + vc.getCaso().getId() + ").");
    }

    @Override
    public void eliminarVinculo(Integer id) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar: El vínculo de caso con ID " + id + " no existe."));
        contextoSeguridad.verificarAccesoACaso(vc.getCaso().getId());
        // A9: solo admin o Principal autorizado del caso (sin la regla de
        // auto-autorización de las transiciones de estado: borrar no autoriza).
        exigirAdminOPrincipalAutorizado(vc.getCaso().getId());
        boolean esPrincipal = vc.getVinculo() != null
                && VinculoCaso_Repository.VINCULO_PRINCIPAL
                        .equalsIgnoreCase(vc.getVinculo().getNombre());
        if (esPrincipal && !contextoSeguridad.esAdmin()) {
            // Si es el ÚNICO Principal autorizado del caso, también solo admin;
            // queda cubierto por la regla general de abajo, pero se distingue
            // el mensaje porque el caso queda sin Principal hasta que se
            // vincule otro.
            boolean esUnicoPrincipalAutorizado =
                    vc.getEstado() == EstadoVinculo.AUTORIZADO
                            && vinculoCasoRepository.findPrincipalesDelCaso(
                                    vc.getCaso().getId(), EstadoVinculo.AUTORIZADO,
                                    VinculoCaso_Repository.VINCULO_PRINCIPAL).size() == 1;
            if (esUnicoPrincipalAutorizado) {
                throw new SecurityException(
                        "Solo un administrador puede eliminar el único abogado principal autorizado del caso.");
            }
            throw new SecurityException(
                    "Solo un administrador puede eliminar el vínculo del abogado principal del caso.");
        }
        // Si el objetivo era el (único) Principal, el caso queda sin Principal
        // hasta que se vincule otro: la autorización del siguiente Principal
        // la decide el admin (o el Principal que se vincule después).
        Integer idCaso = vc.getCaso().getId();
        Integer idPersonaVinculada = vc.getPersonaFirma() != null ? vc.getPersonaFirma().getId() : null;
        vinculoCasoRepository.delete(vc);
        auditar(idCaso,
                "Vínculo eliminado: vínculo " + id
                        + " (persona " + idPersonaVinculada
                        + ", caso " + idCaso + ").");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Integer idPersona, Integer idCaso) {
        // La consulta es sobre el propio acceso, salvo administradores.
        contextoSeguridad.verificarMismaPersona(idPersona);
        Integer yo = contextoSeguridad.idPersonaAutenticada();
        if (contextoSeguridad.esAdmin() && !idPersona.equals(yo)) {
            // M12: un admin que consulta el acceso de OTRA persona solo ve
            // casos de su firma: el caso debe pertenecer a su firma, o la
            // persona consultada debe pertenecer a su firma (cuando su firma
            // sea resoluble). En caso contrario, 404 genérico.
            // V5: el súper admin no se acota (ve cualquier caso/persona).
            // La firma de la persona consultada se resuelve por abogado o
            // por personas.id_firma (antes solo por abogado: un admin no
            // abogado quedaba fuera del chequeo).
            if (!contextoSeguridad.esSuperAdmin()) {
                Caso caso = casoRepository.findById(idCaso)
                        .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
                Optional<Integer> firmaAdmin = contextoSeguridad.idFirmaAutenticada();
                boolean casoDeSuFirma = firmaAdmin.isPresent()
                        && caso.getFirma() != null
                        && firmaAdmin.get().equals(caso.getFirma().getId());
                boolean personaDeSuFirma = firmaAdmin.isPresent()
                        && contextoSeguridad.resolverIdFirmaDePersona(idPersona)
                                .map(firmaPersona -> firmaPersona.equals(firmaAdmin.get()))
                                .orElse(false);
                if (!casoDeSuFirma && !personaDeSuFirma) {
                    throw new EntityNotFoundException("Recurso no encontrado");
                }
            }
        }
        return vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndEstado(
                idPersona, idCaso, EstadoVinculo.AUTORIZADO);
    }

    // =========================================================================
    // Guardas y auditoría compartidas (C7 / A9 / M13)
    // =========================================================================

    /**
     * Exige que el usuario autenticado sea administrador o el Principal
     * AUTORIZADO del caso. Para las transiciones de estado (C7) se usa
     * dentro de {@link #cargarParaGobierno(Integer)}, que además deniega la
     * auto-autorización; para eliminar (A9) se usa directamente.
     */
    private void exigirAdminOPrincipalAutorizado(Integer idCaso) {
        if (contextoSeguridad.esAdmin()) {
            return;
        }
        Integer yo = contextoSeguridad.idPersonaAutenticada();
        boolean soyPrincipalAutorizado = vinculoCasoRepository
                .findPrincipalesDelCaso(idCaso, EstadoVinculo.AUTORIZADO,
                        VinculoCaso_Repository.VINCULO_PRINCIPAL)
                .stream()
                .anyMatch(p -> p.getPersonaFirma() != null
                        && yo.equals(p.getPersonaFirma().getId()));
        if (!soyPrincipalAutorizado) {
            throw new SecurityException(
                    "Solo un administrador o el abogado principal del caso puede realizar esta acción.");
        }
    }

    /**
     * Carga un vínculo y aplica las guardas comunes de gobierno (C7):
     * acceso al caso, actor admin o Principal autorizado del caso, sin
     * auto-autorización (nadie gobierna su propio vínculo salvo admin), y
     * revalidación de persona, cliente y caso activos antes del cambio.
     */
    private Vinculo_Caso cargarParaGobierno(Integer idVinculoCaso) {
        Vinculo_Caso vc = vinculoCasoRepository.findById(idVinculoCaso)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de caso no encontrado con ID: " + idVinculoCaso));
        contextoSeguridad.verificarAccesoACaso(vc.getCaso().getId());
        if (!contextoSeguridad.esAdmin()) {
            Integer yo = contextoSeguridad.idPersonaAutenticada();
            // Sin auto-autorización: el titular del vínculo no puede
            // autorizarse, desautorizarse ni rechazarse a sí mismo.
            if (vc.getPersonaFirma() != null && yo.equals(vc.getPersonaFirma().getId())) {
                throw new SecurityException("No puede gestionar su propio vínculo.");
            }
            exigirAdminOPrincipalAutorizado(vc.getCaso().getId());
        }
        // Revalidar activos antes del cambio de estado.
        if (vc.getPersonaFirma() != null && Boolean.FALSE.equals(vc.getPersonaFirma().getActivo())) {
            throw new IllegalArgumentException("La persona del vínculo está inactiva.");
        }
        if (vc.getCliente() != null && Boolean.FALSE.equals(vc.getCliente().getActivo())) {
            throw new IllegalArgumentException("El cliente del vínculo está inactivo.");
        }
        if (vc.getCaso() != null && !Boolean.TRUE.equals(vc.getCaso().getActivo())) {
            throw new IllegalArgumentException("El caso del vínculo está inactivo.");
        }
        return vc;
    }

    /**
     * Registra la transición de gobierno en el historial del caso (M13/A9).
     * Si el registro falla, NO revierte la operación principal: solo se
     * loguea el fallo.
     */
    private void auditar(Integer idCaso, String descripcion) {
        try {
            // HistorialCambioRequestDTO.fecha es LocalDateTime; el servicio de
            // historial ignora el idPersona del DTO y firma con el usuario
            // autenticado.
            historialCambioService.guardar(new HistorialCambioRequestDTO(
                    LocalDateTime.now(), descripcion, idCaso,
                    contextoSeguridad.idPersonaAutenticada()));
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría del vínculo en el caso {}: {}",
                    idCaso, e.getMessage());
        }
    }
}
