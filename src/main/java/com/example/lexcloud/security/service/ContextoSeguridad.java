package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.EstadoVinculo;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Cliente;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Rol;
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
 * esa persona o por {@code personas.id_firma} (migración V5).
 *
 * <p><b>Modelo de autorización (V5, tres niveles):</b></p>
 * <ul>
 *   <li>{@code SUPER_ADMIN}: global, sin firma. Nunca se acota a una firma:
 *       {@link #idFirmaAutenticada()} devuelve vacío para él aunque su
 *       persona tuviera {@code id_firma} asignado por error. Solo él crea o
 *       edita firmas, crea personas con rol admin y gestiona roles y
 *       catálogos globales.</li>
 *   <li>{@code ADMINISTRADOR} (de firma): acotado a su firma, resuelta por
 *       abogado activo o por {@code personas.id_firma}. Gestiona personas,
 *       casos, clientes y agendas de su firma y crea logins solo para
 *       personas de su firma. Nunca crea otra firma ni otro admin.</li>
 *   <li>El resto (abogado sin admin y demás roles): operativo en su firma;
 *       no crea usuarios ni logins.</li>
 *   <li>El administrador lo decide el flag {@code Rol.esAdmin} (migración V4),
 *       no el nombre del rol: comparar nombres es frágil y el nombre en el JWT
 *       puede quedar desactualizado si el rol cambia tras el login. La
 *       excepción es {@link #esSuperAdmin()}: el nivel súper admin SÍ se
 *       distingue por NOMBRE de rol ({@code SUPER_ADMIN}), porque
 *       {@code ADMINISTRADOR} también es {@code es_admin} y el flag solo no
 *       los separa.</li>
 *   <li>Excepción bootstrap (legado): un administrador SIN firma resoluble
 *       (p. ej. el primer usuario antes de migrar su cuenta a SUPER_ADMIN)
 *       opera en <b>cualquier</b> firma existente salvo la del sistema. En
 *       cuanto tenga registro de abogado o {@code personas.id_firma}, queda
 *       acotado a su firma como cualquier admin.</li>
 *   <li>Una persona sin registro de abogado ni {@code personas.id_firma} no
 *       tiene firma resoluble: los métodos que exigen firma deniegan en ese
 *       caso (fail-closed), salvo {@link #verificarAccesoACaso(Integer)}, donde
 *       el acceso también se puede verificar por vínculo autorizado.</li>
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

    /** Nombre exacto del rol súper administrador (seed de la migración V5). */
    public static final String ROL_SUPER_ADMIN = "SUPER_ADMIN";

    /** Nombre exacto del rol administrador de firma (seed de la migración V4). */
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    /**
     * Verdadero si el rol es de nivel administrador: flag {@code es_admin}
     * o nombre {@code ADMINISTRADOR} / {@code SUPER_ADMIN}. El nombre se
     * comprueba además del flag por defensa en profundidad: un rol admin
     * renombrado por BD seguiría siendo admin por el flag, y un rol con
     * nombre admin pero flag en falso no debe colarse como no-admin.
     */
    public static boolean esRolAdmin(Rol rol) {
        if (rol == null) {
            return false;
        }
        return Boolean.TRUE.equals(rol.getEsAdmin())
                || esRolSuperAdmin(rol)
                || ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
    }

    /** Verdadero si el rol es el súper administrador (por NOMBRE exacto). */
    public static boolean esRolSuperAdmin(Rol rol) {
        return rol != null && rol.getNombre() != null
                && ROL_SUPER_ADMIN.equalsIgnoreCase(rol.getNombre().trim());
    }

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
     * Cubre a AMBOS niveles de admin ({@code SUPER_ADMIN} y
     * {@code ADMINISTRADOR}); para distinguirlos, usar {@link #esSuperAdmin()}.
     */
    public boolean esAdmin() {
        return personaRepository.findById(idPersonaAutenticada())
                .map(persona -> persona.getRol() != null
                        && Boolean.TRUE.equals(persona.getRol().getEsAdmin()))
                .orElse(false);
    }

    /**
     * Verdadero si el usuario autenticado es súper administrador, por
     * NOMBRE de rol ({@code SUPER_ADMIN}), resuelto contra la base de datos
     * como {@link #esAdmin()}. Por nombre y no solo por {@code es_admin}:
     * {@code ADMINISTRADOR} también tiene el flag en true y el flag solo no
     * separa el nivel global del nivel por firma.
     */
    public boolean esSuperAdmin() {
        return personaRepository.findById(idPersonaAutenticada())
                .map(persona -> esRolSuperAdmin(persona.getRol()))
                .orElse(false);
    }

    /**
     * Firma de una persona cualquiera (no necesariamente la autenticada),
     * en el mismo orden que la resolución del propio usuario: firma del
     * abogado activo (si hay) → {@code personas.id_firma} → ninguna.
     * La usa la guarda de registro (la firma de la persona objetivo debe
     * ser la del admin de firma que la registra) y el acotado por firma de
     * personas. No aplica la regla global del súper admin: aquí se resuelve
     * la firma DE LA PERSONA, no el alcance del llamador.
     */
    public Optional<Integer> resolverIdFirmaDePersona(Integer idPersona) {
        if (idPersona == null) {
            return Optional.empty();
        }
        return personaRepository.findById(idPersona)
                .flatMap(this::firmaDePersona);
    }

    /**
     * Orden de resolución de firma de una persona: el abogado manda (para
     * no romper los flujos actuales) y {@code personas.id_firma} cubre a
     * los no abogados. Si hay inconsistencia (abogado en firma A e
     * {@code id_firma} = B), gana el abogado.
     */
    private Optional<Integer> firmaDePersona(Persona persona) {
        if (persona == null) {
            return Optional.empty();
        }
        // N3: solo un registro de abogado ACTIVO resuelve firma.
        Optional<Abogado> abogado = abogadoRepository.findByPersonaIdActivo(persona.getId());
        if (abogado.isPresent() && abogado.get().getFirma() != null) {
            return Optional.of(abogado.get().getFirma().getId());
        }
        return Optional.ofNullable(persona.getIdFirma());
    }

    /**
     * Id de la firma del usuario autenticado.
     * Orden: firma del abogado activo (si hay) → {@code personas.id_firma}
     * → ninguna. El súper admin SIEMPRE devuelve vacío: es global por
     * definición y nunca se acota a una firma, aunque su persona tuviera
     * {@code id_firma} asignado por error. Gracias a eso, todas las ramas
     * "admin sin firma → global" de los servicios tratan al súper admin
     * como global sin tocar cada una.
     */
    public Optional<Integer> idFirmaAutenticada() {
        Integer yo = idPersonaAutenticada();
        Optional<Persona> persona = personaRepository.findById(yo);
        if (persona.map(p -> esRolSuperAdmin(p.getRol())).orElse(false)) {
            return Optional.empty();
        }
        return persona.flatMap(this::firmaDePersona);
    }

    /**
     * Id de la firma del usuario autenticado, o 403 si no tiene firma resoluble.
     * Usar solo en operaciones que exigen pertenecer a una firma. El súper
     * admin no tiene firma resoluble (es global): los listados que él usa
     * no deben pasar por aquí, sino por la rama "admin sin firma → global"
     * o por una variante global del repositorio.
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
     * <ul>
     *   <li>Súper admin: cualquier firma (es global; no se acota).</li>
     *   <li>Administrador SIN firma resoluble (bootstrap, legado): puede operar en
     *       cualquier firma existente; sin esta excepción toda creación con
     *       idFirma explícita le fallaba con 403 aunque el front la enviara.</li>
     *   <li>Administrador CON firma: solo su propia firma, como cualquier
     *       usuario (el bypass global anterior contradecía el modelo por
     *       firma y dejaba ver datos de otras firmas).</li>
     *   <li>No-admin: solo su propia firma. Fail-closed: si la firma del
     *       usuario no se puede resolver (sin registro de abogado ni
     *       {@code personas.id_firma}), la operación se deniega; nunca se
     *       permite por no poder verificar.</li>
     * </ul>
     */
    public void verificarMismaFirma(Integer idFirma) {
        if (idFirma == null) {
            throw new IllegalArgumentException("Debe indicar la firma.");
        }
        if (esSuperAdmin()) {
            return;
        }
        if (esAdmin() && idFirmaAutenticada().isEmpty()) {
            return;
        }
        Integer propia = idFirmaAutenticadaRequerida();
        if (!propia.equals(idFirma)) {
            throw new SecurityException("No tiene permisos para consultar datos de otra firma.");
        }
    }

    /**
     * Resuelve la firma efectiva para operaciones con idFirma opcional (P0-B2).
     * Precedencia (V5): la explícita, si viene; si no, la del abogado activo;
     * si no hay, {@code personas.id_firma}; si tampoco hay, ninguna.
     * Es la ÚNICA derivación de firma: los ternarios inline de los servicios se
     * sustituyen por este método (N11).
     * <ul>
     *   <li>Explícita distinta de la propia (cuando hay firma resoluble) →
     *       403: nadie opera en otra firma, ni siquiera un admin con firma.
     *       El súper admin no tiene firma propia, así que cualquier
     *       explícita le vale (opera en la firma que elija).</li>
     *   <li>Sin explícita y sin firma resoluble: admin (bootstrap o súper
     *       admin) → null, y el llamador interpreta null como "global" o
     *       rechaza con 400 según la operación; no-admin → 403.</li>
     * </ul>
     * Nunca devuelve una firma distinta de la del llamador, salvo para el
     * admin bootstrap y el súper admin, que no tienen firma propia.
     */
    public Integer resolverIdFirmaEfectiva(Integer idFirmaExplicita) {
        Optional<Integer> propia = idFirmaAutenticada();
        if (idFirmaExplicita != null) {
            if (propia.isPresent() && !propia.get().equals(idFirmaExplicita)) {
                throw new SecurityException("No tiene permisos para operar en otra firma.");
            }
            return idFirmaExplicita;
        }
        if (propia.isPresent()) {
            return propia.get();
        }
        if (esAdmin()) {
            return null;
        }
        throw new SecurityException("No tienes una firma asignada; selecciona una firma");
    }

    /**
     * Exige que el abogado consultado pertenezca a la firma del usuario
     * autenticado (o sea el propio usuario).
     *
     * <ul>
     *   <li>Abogado inexistente ({@code null}) o de otra firma:
     *       {@link EntityNotFoundException} con mensaje genérico, indistinguible
     *       de "no existe" (no revela la existencia del objetivo).</li>
     *   <li>Firma del usuario no resoluble (sin registro de abogado ni
     *       {@code personas.id_firma}):
     *       {@link SecurityException} genérica (fail-closed), salvo el súper
     *       admin (global) y el admin bootstrap (sin firma), que acceden a
     *       cualquier abogado. Es una violación del propio contexto del
     *       llamador y no revela nada del objetivo.</li>
     * </ul>
     */
    public void verificarAccesoAAbogado(Abogado abogado) {
        if (abogado == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        boolean esPropio = abogado.getPersona() != null
                && abogado.getPersona().getId().equals(idPersonaAutenticada());
        if (esPropio) {
            return;
        }
        // Súper admin: acceso global a cualquier abogado.
        if (esSuperAdmin()) {
            return;
        }
        // Bootstrap: un admin sin firma resoluble accede a cualquier abogado.
        if (esAdmin() && idFirmaAutenticada().isEmpty()) {
            return;
        }
        Integer propia = idFirmaAutenticadaRequerida();
        if (abogado.getFirma() == null || !propia.equals(abogado.getFirma().getId())) {
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
     * {@link SecurityException} genérica (fail-closed), salvo el súper
     * admin (global) y el admin bootstrap (sin firma), que acceden a
     * cualquier cliente.
     */
    public void verificarAccesoACliente(Cliente cliente) {
        if (cliente == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        // Súper admin: acceso global a cualquier cliente.
        if (esSuperAdmin()) {
            return;
        }
        // Bootstrap: un admin sin firma resoluble accede a cualquier cliente.
        if (esAdmin() && idFirmaAutenticada().isEmpty()) {
            return;
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
        // Súper admin: acceso global a cualquier caso (salvo el raíz).
        if (esSuperAdmin()) {
            return;
        }
        // Bootstrap: un admin sin firma resoluble accede a cualquier caso
        // (salvo el raíz, ya rechazado arriba).
        if (esAdmin() && idFirmaAutenticada().isEmpty()) {
            return;
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

    /**
     * N7: solo un administrador o el abogado Principal autorizado del caso
     * puede desactivar un caso o llevarlo a un estado terminal. Mismo
     * criterio que VinculoCasoServiceImpl.exigirAdminOPrincipalAutorizado
     * (no se reutiliza directamente para no crear dependencia entre
     * servicios).
     */
    public void exigirAdminOPrincipalAutorizado(Integer idCaso) {
        if (esAdmin()) {
            return;
        }
        Integer yo = idPersonaAutenticada();
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

    /** Verdadero si la persona es la persona del sistema (null-safe). */
    public boolean esPersonaSistema(Persona persona) {
        return persona != null && CC_PERSONA_SISTEMA.equals(persona.getCc());
    }

    /** Verdadero si la firma es la firma del sistema (null-safe). */
    public boolean esFirmaSistema(Firma firma) {
        return firma != null && NIT_FIRMA_SISTEMA.equals(firma.getNit());
    }
}
