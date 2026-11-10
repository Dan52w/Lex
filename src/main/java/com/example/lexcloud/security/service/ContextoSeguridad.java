package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.VinculoCaso_Repository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

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
 *   <li>El rol de administrador es el mismo que reconoce el front
 *       ({@code ADMINISTRADOR}, {@code ADMIN}, {@code ROLE_ADMIN}); solo él
 *       puede usar las variantes globales sin firma (SVC-2/SVC-3).</li>
 *   <li>El administrador es un rol <b>por firma</b>: puede ver cualquier caso
 *       de <i>su</i> firma sin vínculo, pero nunca datos de otra firma.</li>
 *   <li>Una persona sin registro de abogado no tiene firma resoluble: los
 *       métodos que exigen firma lanzan {@link SecurityException} en ese caso,
 *       salvo que el acceso se pueda verificar por vínculo autorizado.</li>
 * </ul>
 */
@Component
public class ContextoSeguridad {

    /** Nombres de rol que el front trata como administrador (ver Sidebar.tsx). */
    private static final Set<String> ROLES_ADMIN = Set.of("ADMINISTRADOR", "ADMIN", "ROLE_ADMIN");

    private final Abogado_Repository abogadoRepository;
    private final Caso_Repository casoRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;

    public ContextoSeguridad(Abogado_Repository abogadoRepository,
                             Caso_Repository casoRepository,
                             VinculoCaso_Repository vinculoCasoRepository) {
        this.abogadoRepository = abogadoRepository;
        this.casoRepository = casoRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
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

    /** Verdadero si el usuario autenticado tiene rol de administrador. */
    public boolean esAdmin() {
        String rol = rolAutenticado();
        return rol != null && ROLES_ADMIN.contains(rol.trim().toUpperCase());
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
     * Si la firma del usuario no se puede resolver (sin registro de abogado),
     * no se puede verificar y se permite la operación: ese caso queda
     * documentado como riesgo residual (el modelo no guarda la firma en
     * Persona, solo en Abogado).
     */
    public void verificarMismaFirma(Integer idFirma) {
        if (idFirma == null) {
            throw new IllegalArgumentException("Debe indicar la firma.");
        }
        Optional<Integer> propia = idFirmaAutenticada();
        if (propia.isPresent() && !propia.get().equals(idFirma)) {
            throw new SecurityException("No tiene permisos para consultar datos de otra firma.");
        }
    }

    /**
     * Exige que el abogado consultado pertenezca a la firma del usuario
     * autenticado (o sea el propio usuario). Si la firma del usuario no se
     * puede resolver, no se puede verificar y se permite la operación.
     */
    public void verificarAccesoAAbogado(Abogado abogado) {
        if (abogado == null) {
            throw new EntityNotFoundException("Abogado no encontrado.");
        }
        Optional<Integer> propia = idFirmaAutenticada();
        boolean esPropio = abogado.getPersona() != null
                && abogado.getPersona().getId().equals(idPersonaAutenticada());
        if (propia.isPresent() && !esPropio
                && !propia.get().equals(abogado.getFirma().getId())) {
            throw new SecurityException("No tiene permisos para consultar datos de un abogado de otra firma.");
        }
    }

    /**
     * Exige acceso al caso: pertenece a la firma del usuario autenticado
     * (esto incluye al administrador de la firma) o el usuario tiene un
     * vínculo autorizado con él (cubre al personal sin registro de abogado).
     * El caso raíz del sistema (id 1) nunca admite esta operación.
     */
    public void verificarAccesoACaso(Integer idCaso) {
        if (idCaso == null) {
            throw new IllegalArgumentException("Debe indicar el caso.");
        }
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado con ID: " + idCaso));
        if (caso.getId() == 1) {
            throw new SecurityException("El caso raíz del sistema no admite esta operación.");
        }
        Optional<Integer> firma = idFirmaAutenticada();
        if (firma.isPresent() && caso.getFirma() != null
                && firma.get().equals(caso.getFirma().getId())) {
            return;
        }
        if (vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(
                idPersonaAutenticada(), idCaso)) {
            return;
        }
        throw new SecurityException("No tiene acceso al caso con ID: " + idCaso);
    }
}
