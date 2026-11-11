package com.example.lexcloud.service;

import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lote 6 (M14) — Flujo común de los 18 servicios de catálogo.
 *
 * <p>Los 18 mappers comparten las firmas {@code toEntity(Req) / toResponse(E) /
 * toResponseList(List<E>)}, pero NO se pudo crear una interfaz común
 * {@code CatalogoMapper} que extendieran: los mappers no están en los archivos
 * permitidos de este lote. Por eso la abstracción no depende de una interfaz de
 * mapper, sino de adaptadores funcionales (referencias a métodos del repositorio,
 * del mapper y de la entidad) que cada impl pasa por constructor. El flujo
 * completo — normalización (A17), duplicados (A16), vetos de seeds (A14),
 * borrado con mensaje veraz (A15), solo-admin en escrituras (C3), búsqueda
 * parcial (M15) y paginación (M1) — vive UNA sola vez aquí.</p>
 *
 * <p>Particularidades por catálogo, vía hooks protegidos:</p>
 * <ul>
 *   <li>{@link #esVisible(Object)} — Tipo_Caso y Subtipo_Caso ocultan 'RAÍZ' (M16).</li>
 *   <li>{@link #preparar(Object)} — Subtipo_Caso carga el Tipo_Caso ANTES de la
 *       comprobación de duplicado (H1/A18) y lo devuelve como contexto.</li>
 *   <li>{@link #validarDuplicado(String, Integer, Object)} — Subtipo_Caso comprueba
 *       por (tipoCaso, nombre) con {@code findByTipoCasoIdAndNombre}.</li>
 *   <li>{@link #crearEntidad(Object, String, Object)} / {@link #aplicarCambios(Object, Object, String, Object)}
 *       — Subtipo_Caso asigna el tipo (vía mapper + setTipoCaso) y prohíbe cambiar
 *       el tipo de un subtipo con casos (M17).</li>
 *   <li>{@link #existeDuplicadoAhora(String, Object)} — re-consulta para distinguir
 *       el 409 "ya existe" de cualquier otra violación de integridad (A16).</li>
 * </ul>
 *
 * <p>Las semillas inmutables se declaran por catálogo como nombres NORMALIZADOS
 * (mayúsculas, sin espacios en los extremos) y el veto compara siempre por ese
 * nombre normalizado, nunca por id (A14).</p>
 *
 * <p>Pendiente opcional (decisión del coordinador, NO aplicado aquí): índice único
 * funcional en BD {@code UNIQUE (lower(trim(nombre)))} para cerrar la condición de
 * carrera entre la comprobación previa y el INSERT. Hoy la defensa es la
 * comprobación previa + el UNIQUE de la columna + la traducción del catch.</p>
 */
public abstract class AbstractCatalogoService<E, Req, Resp> {

    protected final JpaRepository<E, Integer> repositorio;
    protected final ContextoSeguridad contextoSeguridad;
    protected final Set<String> semillasInmutables;
    protected final String nombreCatalogo;

    private final Function<Req, String> nombreDeRequest;
    private final Function<Req, E> toEntity;
    private final Function<E, Resp> toResponse;
    private final Function<List<E>, List<Resp>> toResponseList;
    private final Function<E, String> getNombre;
    private final BiConsumer<E, String> setNombre;
    private final Function<E, Integer> getId;
    private final Function<String, Optional<E>> buscarExacto;
    private final Function<String, List<E>> buscarParcial;
    private final BiFunction<String, Pageable, List<E>> buscarParcialPaginado;

    protected AbstractCatalogoService(JpaRepository<E, Integer> repositorio,
                                      ContextoSeguridad contextoSeguridad,
                                      Set<String> semillasInmutables,
                                      String nombreCatalogo,
                                      Function<Req, String> nombreDeRequest,
                                      Function<Req, E> toEntity,
                                      Function<E, Resp> toResponse,
                                      Function<List<E>, List<Resp>> toResponseList,
                                      Function<E, String> getNombre,
                                      BiConsumer<E, String> setNombre,
                                      Function<E, Integer> getId,
                                      Function<String, Optional<E>> buscarExacto,
                                      Function<String, List<E>> buscarParcial,
                                      BiFunction<String, Pageable, List<E>> buscarParcialPaginado) {
        this.repositorio = repositorio;
        this.contextoSeguridad = contextoSeguridad;
        this.semillasInmutables = Set.copyOf(semillasInmutables);
        this.nombreCatalogo = nombreCatalogo;
        this.nombreDeRequest = nombreDeRequest;
        this.toEntity = toEntity;
        this.toResponse = toResponse;
        this.toResponseList = toResponseList;
        this.getNombre = getNombre;
        this.setNombre = setNombre;
        this.getId = getId;
        this.buscarExacto = buscarExacto;
        this.buscarParcial = buscarParcial;
        this.buscarParcialPaginado = buscarParcialPaginado;
    }

    // =========================================================================
    // A17 — Normalización: trim() siempre, antes de comparar y de persistir.
    // =========================================================================

    /** Nombre con espacios extremos eliminados; {@code null} se trata como vacío. */
    public static String normalizar(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    /** Clave de comparación de vetos y semillas: normalizado + mayúsculas. */
    public static String normalizarClave(String nombre) {
        return normalizar(nombre).toUpperCase(Locale.ROOT);
    }

    /** ¿El nombre normalizado de esta entidad es una semilla inmutable? */
    protected boolean esSemilla(E entidad) {
        return semillasInmutables.contains(normalizarClave(getNombre.apply(entidad)));
    }

    // =========================================================================
    // C3 — Escrituras solo admin; las lecturas quedan abiertas.
    // =========================================================================

    /** Exige rol admin al inicio de guardar/actualizar/eliminar. */
    protected void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede modificar los catálogos.");
        }
    }

    // =========================================================================
    // Hooks de particularidades por catálogo (por defecto, comportamiento común).
    // =========================================================================

    /**
     * Carga dependencias ANTES de la comprobación de duplicado y devuelve el
     * contexto que luego reciben {@link #crearEntidad} y {@link #aplicarCambios}.
     * Por defecto no hace nada (devuelve {@code null}).
     */
    protected Object preparar(Req request) {
        return null;
    }

    /** ¿Esta entidad es visible en los listados? Por defecto todas lo son. */
    protected boolean esVisible(E entidad) {
        return true;
    }

    /**
     * Comprueba el duplicado por nombre normalizado (case-insensitive, vía
     * {@code findByNombreIgnoreCase}). {@code excluirId} excluye a la propia
     * entidad en actualizar; {@code null} en guardar.
     */
    protected void validarDuplicado(String nombreNormalizado, Integer excluirId, Req request) {
        buscarExacto.apply(nombreNormalizado)
                .filter(otro -> excluirId == null || !excluirId.equals(getId.apply(otro)))
                .ifPresent(otro -> {
                    throw new IllegalStateException("El " + nombreCatalogo + " con el nombre '"
                            + nombreNormalizado + "' ya existe.");
                });
    }

    /** Re-consulta de duplicado para el catch de {@link #guardarConTraduccion}. */
    protected boolean existeDuplicadoAhora(String nombreNormalizado, Req request) {
        return buscarExacto.apply(nombreNormalizado).isPresent();
    }

    /** Construye la entidad desde el request, ya con el nombre normalizado. */
    protected E crearEntidad(Req request, String nombreNormalizado, Object contexto) {
        E entidad = toEntity.apply(request);
        setNombre.accept(entidad, nombreNormalizado);
        return entidad;
    }

    /** Aplica los cambios de actualizar sobre la entidad cargada. */
    protected void aplicarCambios(E entidad, Req request, String nombreNormalizado, Object contexto) {
        setNombre.accept(entidad, nombreNormalizado);
    }

    // =========================================================================
    // A14 — Vetos de seeds: no renombrar ni borrar una semilla (por nombre
    // normalizado, nunca por id).
    // =========================================================================

    /** Veto de renombre: una semilla no puede cambiar de nombre. */
    protected void verificarVetoRenombre(E entidad, String nombreNormalizado) {
        if (esSemilla(entidad) && !normalizarClave(getNombre.apply(entidad)).equals(normalizarClave(nombreNormalizado))) {
            throw new IllegalStateException("El valor '" + getNombre.apply(entidad)
                    + "' es del sistema y no se puede renombrar.");
        }
    }

    /** Veto de borrado: una semilla no se puede eliminar. */
    protected void verificarVetoEliminacion(E entidad) {
        if (esSemilla(entidad)) {
            throw new IllegalStateException("No se puede eliminar el valor '" + getNombre.apply(entidad)
                    + "': es un valor del sistema.");
        }
    }

    // =========================================================================
    // A16 — Traducción de la violación de integridad: re-consulta y distingue
    // el 409 "ya existe" de cualquier otra violación (que se encadena, nunca
    // se traga).
    // =========================================================================

    protected E guardarConTraduccion(Supplier<E> guardado, String nombreNormalizado, Req request) {
        try {
            return guardado.get();
        } catch (DataIntegrityViolationException ex) {
            if (existeDuplicadoAhora(nombreNormalizado, request)) {
                throw new IllegalStateException("El " + nombreCatalogo + " con el nombre '"
                        + nombreNormalizado + "' ya existe.");
            }
            throw new IllegalStateException("No se pudo guardar el " + nombreCatalogo
                    + " con el nombre '" + nombreNormalizado + "'.", ex);
        }
    }

    // =========================================================================
    // Escrituras (C3: solo admin).
    // =========================================================================

    public Resp guardar(Req request) {
        exigirAdmin();
        String nombre = normalizar(nombreDeRequest.apply(request));
        Object contexto = preparar(request);
        validarDuplicado(nombre, null, request);
        E entidad = crearEntidad(request, nombre, contexto);
        return toResponse.apply(guardarConTraduccion(() -> repositorio.save(entidad), nombre, request));
    }

    public Resp actualizar(Integer id, Req request) {
        exigirAdmin();
        return repositorio.findById(id).map(entidad -> {
            String nombre = normalizar(nombreDeRequest.apply(request));
            verificarVetoRenombre(entidad, nombre);
            Object contexto = preparar(request);
            validarDuplicado(nombre, id, request);
            aplicarCambios(entidad, request, nombre, contexto);
            return toResponse.apply(guardarConTraduccion(() -> repositorio.save(entidad), nombre, request));
        }).orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
    }

    /**
     * A15 — Eliminar con mensaje veraz: carga la entidad (404 si no existe),
     * aplica el veto de seed y borra con {@code delete(entidad)} (no
     * {@code deleteById}, para no incurrir en el TOCTOU con
     * {@code EmptyResultDataAccessException}). Si la BD rechaza el borrado por
     * referencias, 409 veraz con la causa encadenada.
     */
    public void eliminar(Integer id) {
        exigirAdmin();
        E entidad = repositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        verificarVetoEliminacion(entidad);
        try {
            repositorio.delete(entidad);
            // A15: flush dentro del try para que la violación FK aflore aquí
            // (sin flush, saltaría en el commit, fuera del catch, y el 409
            // veraz no estaría garantizado).
            repositorio.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar: el valor está en uso por clientes, equipos o casos.", ex);
        }
    }

    // =========================================================================
    // Lecturas (abiertas). M1: las versiones sin parámetros delegan a
    // Paginacion.TAMANO_POR_DEFECTO; las respuestas siguen siendo List.
    // =========================================================================

    public List<Resp> listarTodo() {
        return listarTodo(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    public List<Resp> listarTodo(int page, int size) {
        return toResponseList.apply(repositorio.findAll(Paginacion.crear(page, size)).getContent()
                .stream().filter(this::esVisible).toList());
    }

    /**
     * M15 — Búsqueda PARCIAL case-insensitive: el término se normaliza y se
     * escapa con {@link BusquedaUtil#escaparLike} antes de llegar al LIKE con
     * {@code ESCAPE '\'} del repositorio.
     */
    public List<Resp> listarPorNombre(String nombre) {
        return listarPorNombre(nombre, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    public List<Resp> listarPorNombre(String nombre, int page, int size) {
        String termino = BusquedaUtil.escaparLike(normalizar(nombre));
        return toResponseList.apply(buscarParcialPaginado.apply(termino, Paginacion.crear(page, size))
                .stream().filter(this::esVisible).toList());
    }

    /** Búsqueda EXACTA (se conserva para duplicados y búsqueda exacta). */
    public Optional<Resp> buscarPorNombre(String nombre) {
        return buscarExacto.apply(normalizar(nombre)).map(toResponse);
    }

    public Optional<Resp> buscarPorId(Integer id) {
        return repositorio.findById(id).filter(this::esVisible).map(toResponse);
    }

    /** Búsqueda parcial sin paginar (la usa {@link #listarPorNombre(String)} si un impl la necesita). */
    protected List<E> buscarParcialSinPaginar(String nombre) {
        return buscarParcial.apply(BusquedaUtil.escaparLike(normalizar(nombre)));
    }
}
