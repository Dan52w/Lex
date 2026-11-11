package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.EstadoVinculo;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Cliente;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.VinculoCaso_Repository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resuelve la identidad del usuario autenticado (JWT) para los servicios.
 *
 * Es el único sitio que lee el {@code SecurityContextHolder}: ningún servicio
 * debe resolver el usuario por su cuenta. El principal es {@link UserDetailsImpl}
 * (id de persona, rol), y la firma se resuelve por el registro de abogado de
 * esa persona.
 *
 * <p><b>Modelo de autorización de esta fase:</b></p>
 * <ul>
 *   <li>El administrador lo decide el flag {@code Rol.esAdmin} (migración V4),
 *       no el nombre del rol: comparar nombres es frágil y el nombre en el JWT
 *       puede quedar desactualizado si el rol cambia tras el login.</li>
 *   <li>El administrador es un rol <b>por firma</b>: puede ver cualquier caso
 *       de <i>su</i> firma sin vínculo, pero nunca datos de otra firma.</li>
 *   <li>Una persona sin registro de abogado no tiene firma resoluble: los
 *       métodos que exigen firma deniegan en ese caso (fail-closed), salvo
 *       {@link #verificarAccesoACaso(Integer)}, donde el acceso también se
 *       puede verificar por vínculo autorizado.</li>
 *   <li>Cuando un recurso existe pero el usuario no tiene acceso, la respuesta
 *       es indistinguible de "no existe": {@link EntityNotFoundException}
 *       con mensaje genérico, nunca un 403 que confirme la existencia.</li>
 * </ul>
 */
@Component
public class ContextoSeguridad {

    /** NIT de la firma del sistema, sembrada por la migración V1 ('FIRMA SISTEMA'). */
    public static final String NIT_FIRMA_SISTEMA = "0000000000";

    /** Cédula de la persona del sistema, sembrada por la migración V1. */
    public static final String CC_PERSONA_SISTEMA = "0000000000";

    private final Abogado_Repository abogadoRepository;
    private final Caso_Repository casoRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Persona_Repository personaRepository;

    public ContextoSeguridad(Abogado_Repository abogadoRepository,
                             Caso_Repository casoRepository,
                             VinculoCaso_Repository vinculoCasoRepository,
                             Persona_Repository personaRepository) {
        this.abogadoRepository = abogadoRepository;
        this.casoRepository = casoRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.personaRepository = personaRepository;
    }

    /** Id de persona del usuario autenticado (el id que el login devuelve al front). */
    public Integer idPersonaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new SecurityException("No hay una sesión autenticada válida.");
        }
        return principal.getId();
    }

    /** Nombre del rol del usuario autenticado (tal como está en la base de datos). */
    public String rolAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new SecurityException("No hay una sesión autenticada válida.");
        }
        return principal.getNombreRol();
    }

    /** Correo del usuario autenticado (el username del principal). */
    public String correoAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new SecurityException("No hay una sesión autenticada válida.");
        }
        return principal.getUsername();
    }

    /**
     * Verdadero si el rol vigente de la persona autenticada tiene el flag
     * {@code es_admin}. Se resuelve contra la base de datos (no contra el
     * nombre del rol en el JWT) para que un cambio de rol o de flag tenga
     * efecto sin esperar a que el usuario vuelva a iniciar sesión.
     */
    public boolean esAdmin() {
        return personaRepository.findById(idPersonaAutenticada())
                .map(persona -> persona.getRol() != null
                        && Boolean.TRUE.equals(persona.getRol().getEsAdmin()))
                .orElse(false);
    }

    /**
     * Id de la firma del usuario autenticado, resuelto por su registro de abogado.
     * Vacío si la persona no tiene registro de abogado (personal sin firma resoluble).
     */
    public Optional<Integer> idFirmaAutenticada() {
        return abogadoRepository.findByPersonaId(idPersonaAutenticada())
                .map(abogado -> abogado.getFirma().getId());
    }

    /**
     * Id de la firma del usuario autenticado, o 403 si no tiene firma resoluble.
     * Usar solo en operaciones que exigen pertenecer a una firma.
     */
    public Integer idFirmaAutenticadaRequerida() {
        return idFirmaAutenticada()
                .orElseThrow(() -> new SecurityException(
                        "El usuario autenticado no tiene una firma asociada."));
    }

    /**
     * Exige que el id de persona consultado sea el del usuario autenticado,
     * salvo administradores (que solo ven datos de su propia firma; el
     * filtrado por firma lo hace el servicio que llama).
     */
    public void verificarMismaPersona(Integer idPersona) {
        if (idPersona == null) {
            throw new IllegalArgumentException("Debe indicar la persona.");
        }
        if (!idPersona.equals(idPersonaAutenticada()) && !esAdmin()) {
            throw new SecurityException("No tiene permisos para consultar datos de otra persona.");
        }
    }

    /**
     * Exige que la firma consultada sea la del usuario autenticado.
     * Fail-closed: si la firma del usuario no se puede resolver (sin registro
     * de abogado), la operación se deniega; nunca se permite por no poder
     * verificar.
     */
    public void verificarMismaFirma(Integer idFirma) {
        if (idFirma == null) {
            throw new IllegalArgumentException("Debe indicar la firma.");
        }
        Integer propia = idFirmaAutenticadaRequerida();
        if (!propia.equals(idFirma)) {
            throw new SecurityException("No tiene permisos para consultar datos de otra firma.");
        }
    }

    /**
     * Exige que el abogado consultado pertenezca a la firma del usuario
     * autenticado (o sea el propio usuario).
     *
     * <ul>
     *   <li>Abogado inexistente ({@code null}) o de otra firma:
     *       {@link EntityNotFoundException} con mensaje genérico, indistinguible
     *       de "no existe" (no revela la existencia del objetivo).</li>
     *   <li>Firma del usuario no resoluble (sin registro de abogado):
     *       {@link SecurityException} genérica (fail-closed). Es una violación
     *       del propio contexto del llamador y no revela nada del objetivo.</li>
     * </ul>
     */
    public void verificarAccesoAAbogado(Abogado abogado) {
        if (abogado == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        Integer propia = idFirmaAutenticadaRequerida();
        boolean esPropio = abogado.getPersona() != null
                && abogado.getPersona().getId().equals(idPersonaAutenticada());
        if (!esPropio && (abogado.getFirma() == null
                || !propia.equals(abogado.getFirma().getId()))) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
    }

    /**
     * Exige acceso al abogado por id: lo carga (inexistente →
     * {@link EntityNotFoundException} genérico) y delega en
     * {@link #verificarAccesoAAbogado(Abogado)}.
     */
    public void verificarAccesoAAbogadoPorId(Integer idAbogado) {
        if (idAbogado == null) {
            throw new IllegalArgumentException("Debe indicar el abogado.");
        }
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        verificarAccesoAAbogado(abogado);
    }

    /**
     * Exige que el cliente consultado pertenezca a la firma del usuario
     * autenticado. Cliente inexistente ({@code null}), sin firma o de otra
     * firma: {@link EntityNotFoundException} con mensaje genérico,
     * indistinguible de "no existe". Firma del usuario no resoluble:
     * {@link SecurityException} genérica (fail-closed).
     */
    public void verificarAccesoACliente(Cliente cliente) {
        if (cliente == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        Integer propia = idFirmaAutenticadaRequerida();
        if (cliente.getFirma() == null || !propia.equals(cliente.getFirma().getId())) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
    }

    /**
     * Exige acceso al caso: pertenece a la firma del usuario autenticado
     * (esto incluye al administrador de la firma) o el usuario tiene un
     * vínculo autorizado con él (cubre al personal sin registro de abogado).
     * El caso raíz del sistema (id 1) nunca admite esta operación.
     *
     * <p>Si el caso no existe o el usuario no tiene acceso, se lanza
     * {@link EntityNotFoundException} con mensaje genérico: ambas respuestas
     * son indistinguibles. La guarda del caso raíz se mantiene centralizada
     * aquí (los {@code if (id == 1)} duplicados de los servicios se eliminan
     * en sus lotes correspondientes).</p>
     */
    public void verificarAccesoACaso(Integer idCaso) {
        if (idCaso == null) {
            throw new IllegalArgumentException("Debe indicar el caso.");
        }
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        if (caso.getId() == 1) {
            throw new SecurityException("El caso raíz del sistema no admite esta operación.");
        }
        Optional<Integer> firma = idFirmaAutenticada();
        if (firma.isPresent() && caso.getFirma() != null
                && firma.get().equals(caso.getFirma().getId())) {
            return;
        }
        if (vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndEstado(
                idPersonaAutenticada(), idCaso, EstadoVinculo.AUTORIZADO)) {
            return;
        }
        throw new EntityNotFoundException("Recurso no encontrado");
    }

    /** Verdadero si la persona es la persona del sistema (null-safe). */
    public boolean esPersonaSistema(Persona persona) {
        return persona != null && CC_PERSONA_SISTEMA.equals(persona.getCc());
    }

    /** Verdadero si la firma es la firma del sistema (null-safe). */
    public boolean esFirmaSistema(Firma firma) {
        return firma != null && NIT_FIRMA_SISTEMA.equals(firma.getNit());
    }
}
