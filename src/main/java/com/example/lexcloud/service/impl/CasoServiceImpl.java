package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.CasoMapper;
import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.CasoService;
import com.example.lexcloud.service.GeneradorRadicados;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CasoServiceImpl implements CasoService {

    /** Zona horaria de operación (B6). */
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    /**
     * Estados terminales de un caso (A4). Un caso no puede crearse en uno de
     * estos estados ni salir de uno de ellos. Nombres en mayúsculas; la
     * comparación normaliza a mayúsculas para no depender del case del seed.
     */
    private static final Set<String> ESTADOS_TERMINALES = Set.of(
            "TERMINADO", "CERRADO", "FINALIZADO", "ARCHIVADO");

    private final Caso_Repository casoRepository;
    private final Estado_Repository estadoRepository;
    private final SubtipoCaso_Repository subtipoRepository;
    private final Persona_Repository personaRepository;
    private final Abogado_Repository abogadoRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Vinculo_Repository vinculoRepository;
    private final Cliente_Repository clienteRepository;
    private final HistorialCambio_Repository historialCambioRepository;
    private final Firma_Repository firmaRepository;
    private final CasoMapper casoMapper;
    private final ContextoSeguridad contextoSeguridad;
    private final GeneradorRadicados generadorRadicados;

    public CasoServiceImpl(Caso_Repository casoRepository,
                           Estado_Repository estadoRepository,
                           SubtipoCaso_Repository subtipoRepository,
                           Persona_Repository personaRepository,
                           Abogado_Repository abogadoRepository,
                           VinculoCaso_Repository vinculoCasoRepository,
                           HistorialCambio_Repository historialCambioRepository,
                           Vinculo_Repository vinculoRepository,
                           Cliente_Repository clienteRepository,
                           Firma_Repository firmaRepository,
                           CasoMapper casoMapper,
                           ContextoSeguridad contextoSeguridad,
                           GeneradorRadicados generadorRadicados) {
        this.casoRepository = casoRepository;
        this.estadoRepository = estadoRepository;
        this.subtipoRepository = subtipoRepository;
        this.personaRepository = personaRepository;
        this.abogadoRepository = abogadoRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.historialCambioRepository = historialCambioRepository;
        this.vinculoRepository = vinculoRepository;
        this.clienteRepository = clienteRepository;
        this.firmaRepository = firmaRepository;
        this.casoMapper = casoMapper;
        this.contextoSeguridad = contextoSeguridad;
        this.generadorRadicados = generadorRadicados;
    }

    @Override
    @Transactional
    public CasoResponseDTO guardar(CasoRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        // P0-B2: idFirma opcional; si no viene, se deriva del token. Sin
        // firma resoluble la creación se rechaza con 400 claro.
        Integer idFirma = request.idFirma() != null ? request.idFirma()
                : contextoSeguridad.idFirmaAutenticada().orElse(null);
        if (idFirma == null) {
            throw new IllegalArgumentException("No tienes una firma asignada; selecciona una firma");
        }
        Validaciones.requerirId(request.idCliente(), "idCliente");
        Validaciones.requerirId(request.idSubtipo(), "idSubtipo");
        // idEstado puede venir null: sin estado indicado el caso nace
        // SIN DEFINIR (A4). idCasoPadre es opcional (null = caso raíz padre).

        // El caso se radica en la firma del usuario autenticado.
        contextoSeguridad.verificarMismaFirma(idFirma);

        // C6: el cliente se carga y se verifica ANTES del primer save.
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        contextoSeguridad.verificarAccesoACliente(cliente);
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente no está activo.");
        }

        Caso caso = casoMapper.toEntity(request);

        // A4: no se puede crear un caso en estado terminal. Sin idEstado el
        // caso nace en el estado inicial SIN DEFINIR.
        Estado estado;
        if (request.idEstado() != null) {
            estado = estadoRepository.findById(request.idEstado())
                    .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
            if (esTerminal(estado)) {
                throw new IllegalArgumentException("No se puede crear un caso en un estado terminal.");
            }
        } else {
            estado = estadoRepository.findByNombre("SIN DEFINIR")
                    .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        }
        caso.setEstado(estado);
        caso.setSubtipo_Caso(subtipoRepository.findById(request.idSubtipo())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado")));

        // C6: el padre se verifica ANTES del primer save. verificarAccesoACaso
        // ya rechaza el caso raíz como padre explícito. Sin padre indicado el
        // padre es el caso raíz del sistema, como antes.
        Caso padre;
        if (request.idCasoPadre() != null) {
            contextoSeguridad.verificarAccesoACaso(request.idCasoPadre());
            padre = casoRepository.findById(request.idCasoPadre())
                    .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
            if (padre.getFirma() == null || !idFirma.equals(padre.getFirma().getId())) {
                throw new EntityNotFoundException("Recurso no encontrado");
            }
            if (!Boolean.TRUE.equals(padre.getActivo())) {
                throw new IllegalArgumentException("El caso padre no está activo.");
            }
        } else {
            padre = casoRepository.findById(1)
                    .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        }
        caso.setCasoPadre(padre);

        caso.setFirma(firmaRepository.findById(idFirma)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado")));

        // C5 + M11: radicados desde la secuencia, ANTES del primer insert y
        // con UN solo save. El DTO no trae radicado interno (el mapper lo
        // ignora), así que el interno siempre se genera. El judicial: si
        // viene (23 dígitos, lo exige el DTO) se comprueba unicidad pre-save;
        // si no viene, se deriva del mismo número de secuencia. La rama que
        // trataba "0" como "generar" se eliminó: el DTO ya exige ^\d{23}$.
        GeneradorRadicados.Radicados radicados = generadorRadicados.generar();
        caso.setRadicadoInterno(radicados.interno());
        String judicialRequest = request.radicadoJudicial();
        if (judicialRequest == null || judicialRequest.isBlank()) {
            caso.setRadicadoJudicial(radicados.judicial());
        } else {
            if (casoRepository.existsByRadicadoJudicial(judicialRequest)) {
                throw new IllegalStateException("Ya existe un caso con ese radicado judicial.");
            }
            caso.setRadicadoJudicial(judicialRequest);
        }

        Caso casoGuardado;
        try {
            casoGuardado = casoRepository.save(caso);
        } catch (DataIntegrityViolationException ex) {
            // Defensa en profundidad: una carrera entre dos peticiones puede
            // colisionar en la UNIQUE de radicados aunque el pre-chequeo pasara.
            throw new IllegalStateException("Ya existe un caso con ese radicado.", ex);
        }

        // El creador del vínculo 'Principal' es el usuario autenticado, no el
        // idPersonaCreador que viene en el DTO (el front lo envía, pero es un
        // dato del cliente: aceptar otro id crearía el vínculo a nombre de
        // otra persona). El vínculo nace AUTORIZADO porque el propio creador
        // lo autoriza al radicar el caso (ver VinculoCasoServiceImpl).
        crearVinculo(casoGuardado, contextoSeguridad.idPersonaAutenticada(), cliente);

        return casoMapper.toResponse(casoGuardado);
    }

    @Override
    @Transactional
    public CasoResponseDTO actualizar(Integer id, CasoRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        // M25: verificarAccesoACaso ya rechaza el caso raíz.
        contextoSeguridad.verificarAccesoACaso(id);

        Caso oldCaso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // M9: un caso inactivo no se modifica (reactivarlo es reactivarCaso).
        if (!Boolean.TRUE.equals(oldCaso.getActivo())) {
            throw new IllegalArgumentException("No se puede modificar un caso inactivo.");
        }

        // A4: estos campos no se cambian por PUT. Si el request trae un valor
        // distinto del actual se rechaza en lugar de ignorarlo en silencio.
        // idSubtipo, idFirma e idCasoPadre viven en Caso; idCliente e
        // idPersonaCreador viven en el vínculo 'Principal' del caso.
        if (request.idSubtipo() != null && oldCaso.getSubtipo_Caso() != null
                && !request.idSubtipo().equals(oldCaso.getSubtipo_Caso().getId())) {
            throw new IllegalArgumentException("El subtipo del caso no se puede cambiar.");
        }
        if (request.idFirma() != null && oldCaso.getFirma() != null
                && !request.idFirma().equals(oldCaso.getFirma().getId())) {
            throw new IllegalArgumentException("La firma del caso no se puede cambiar.");
        }
        if (request.idCasoPadre() != null && oldCaso.getCasoPadre() != null
                && !request.idCasoPadre().equals(oldCaso.getCasoPadre().getId())) {
            throw new IllegalArgumentException("El caso padre no se puede cambiar.");
        }
        List<Vinculo_Caso> principales = vinculoCasoRepository
                .findPrincipalesDelCaso(id, EstadoVinculo.AUTORIZADO, "Principal");
        if (!principales.isEmpty()) {
            Vinculo_Caso principal = principales.get(0);
            if (request.idCliente() != null && principal.getCliente() != null
                    && !request.idCliente().equals(principal.getCliente().getId())) {
                throw new IllegalArgumentException("El cliente del caso no se puede cambiar.");
            }
            if (request.idPersonaCreador() != null && principal.getPersonaFirma() != null
                    && !request.idPersonaCreador().equals(principal.getPersonaFirma().getId())) {
                throw new IllegalArgumentException("La persona creadora del caso no se puede cambiar.");
            }
        }

        String estadoAnterior = (oldCaso.getEstado() != null) ? oldCaso.getEstado().getNombre() : "Sin estado";
        String nombreCasoAnterior = (oldCaso.getTitulo() != null) ? oldCaso.getTitulo() : "Sin titulo";

        // CAMPOS PERMITIDOS DE ACTUALIZAR: título, descripción, fecha de
        // inicio, estado (con transición válida) y radicado judicial.
        if (request.titulo() != null) {
            oldCaso.setTitulo(request.titulo());
        }
        if (request.descripcion() != null) {
            oldCaso.setDescripcion(request.descripcion());
        }
        if (request.fecha_Inicio() != null) {
            oldCaso.setFecha_Inicio(request.fecha_Inicio());
        }

        if (request.idEstado() != null) {
            Estado nuevoEstado = estadoRepository.findById(request.idEstado())
                    .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
            // A4: prohibido salir de un terminal; mismo estado = no-op
            // permitido; transición entre no terminales permitida.
            if (esTerminal(oldCaso.getEstado())
                    && (oldCaso.getEstado() == null || !request.idEstado().equals(oldCaso.getEstado().getId()))) {
                throw new IllegalArgumentException("No se puede cambiar el estado de un caso en estado terminal.");
            }
            oldCaso.setEstado(nuevoEstado);
        }

        if (request.radicadoJudicial() != null && !request.radicadoJudicial().isBlank()) {
            if (!request.radicadoJudicial().equals(oldCaso.getRadicadoJudicial())
                    && casoRepository.existsByRadicadoJudicial(request.radicadoJudicial())) {
                throw new IllegalStateException("Ya existe un caso con ese radicado judicial.");
            }
            oldCaso.setRadicadoJudicial(request.radicadoJudicial());
        }

        Caso casoActualizado;
        try {
            casoActualizado = casoRepository.save(oldCaso);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Ya existe un caso con ese radicado.", ex);
        }

        // El autor del historial es el usuario autenticado, no el
        // idPersonaCreador del DTO (el front lo envía con fallback a 1,
        // la persona del sistema, cuando no lo resuelve).
        registrarCambioEnHistorial(casoActualizado, contextoSeguridad.idPersonaAutenticada(),
                "Actualización de datos. Estado anterior: " + estadoAnterior + ". Título anterior: " + nombreCasoAnterior);
        return casoMapper.toResponse(casoActualizado);
    }

    // Métodos de consulta...
    @Override
    public List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona) {
        return listarVinculadosPersonaAutorizado(idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            // La rama admin acota por firma; pero un admin SIN firma
            // resoluble (p. ej. el bootstrap, sin registro de abogado) no
            // puede acotarse: antes esto lanzaba 403 aquí y rompía
            // /mis-casos y /miembro para ese admin. Sin firma se cae a la
            // rama por persona (casos con vínculo autorizado de esa
            // persona), nunca 403. No se cambia el comportamiento del
            // admin CON firma.
            Optional<Integer> idFirma = contextoSeguridad.idFirmaAutenticada();
            if (idFirma.isPresent()) {
                return aResponse(casoRepository.findCasosAutorizadosPorPersonaYFirma(
                        idPersona, idFirma.get(), Paginacion.crear(page, size)));
            }
        }
        return aResponse(casoRepository.findCasosAutorizadosPorPersona(idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarMisCasos(int page, int size) {
        // P0-B2: la persona es la del token; delega en el listado por
        // persona, que ya aplica verificarMismaPersona (pasa: es uno mismo).
        return listarVinculadosPersonaAutorizado(contextoSeguridad.idPersonaAutenticada(), page, size);
    }

    @Override
    public List<CasoResponseDTO> listarPorMiembro(Integer idAbogado, int page, int size) {
        // P0-B2: casos de un miembro. Solo admin o el propio abogado: un
        // compañero de firma NO puede listar los casos de otro miembro por
        // esta vía (verificarAccesoAAbogadoPorId permitiría misma firma).
        Validaciones.requerirId(idAbogado, "idAbogado");
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        Integer idPersonaMiembro = abogado.getPersona() != null ? abogado.getPersona().getId() : null;
        if (idPersonaMiembro == null
                || (!contextoSeguridad.esAdmin()
                        && !idPersonaMiembro.equals(contextoSeguridad.idPersonaAutenticada()))) {
            throw new SecurityException("No tiene permisos para consultar los casos de otro miembro.");
        }
        return listarVinculadosPersonaAutorizado(idPersonaMiembro, page, size);
    }

    @Override
    public List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente) {
        return listarCasosAutorizadosPorAbogadoYCliente(idPersonaFirma, idCliente, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersonaFirma);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.findCasosAutorizadosPorAbogadoYClienteYFirma(
                    idPersonaFirma, idCliente, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.findCasosAutorizadosPorAbogadoYCliente(idPersonaFirma, idCliente, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona) {
        return listarPorTitulo(titulo, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.buscarPorTituloParcialYFirma(
                    BusquedaUtil.escaparLike(titulo), idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.buscarPorTituloParcial(BusquedaUtil.escaparLike(titulo), idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona) {
        return listarPorEstado(estadoNombre, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        // H15: se resuelve el nombre a id y se usa la variante por id.
        // Nombre de estado inexistente -> lista vacía (como antes).
        Integer estadoId = estadoRepository.findByNombre(estadoNombre)
                .map(Estado::getId)
                .orElse(null);
        if (estadoId == null) {
            return List.of();
        }
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.findByEstadoIdAndPersonaYFirma(
                    estadoId, idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.findByEstadoIdAndPersona(estadoId, idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona) {
        return listarPorSubtipo(idSubtipo, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.findBySubtipoIdAndPersonaYFirma(
                    idSubtipo, idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.findBySubtipoIdAndPersona(idSubtipo, idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idSubtipo, Integer idEstado, Integer idPersona) {
        return listarPorSubtipoAndEstado(idSubtipo, idEstado, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idSubtipo, Integer idEstado, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.findByEstadoAndSubtipoAndPersonaYFirma(
                    idPersona, idEstado, idSubtipo, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.findByEstadoAndSubtipoAndPersona(idPersona, idEstado, idSubtipo, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso) {
        return listarPorCasoPadre(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso, int page, int size) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        return aResponse(casoRepository.findByCasoPadreId(idCaso, Paginacion.crear(page, size)));
    }

    @Override
    public CasoResponseDTO buscarPorRadicadoInterno(String radicadoInterno, Integer idPersona) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return casoRepository.findByRadicadoInternoAndPersona(radicadoInterno, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
    }

    @Override
    public CasoResponseDTO buscarPorRadicadoJudicial(String radicadoJudicial, Integer idPersona) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return casoRepository.findByRadicadoJudicialAndPersona(radicadoJudicial, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
    }

    @Override
    public List<CasoResponseDTO> superBuscador(String termino, Integer idPersona) {
        return superBuscador(termino, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> superBuscador(String termino, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.superBuscadorYFirma(
                    BusquedaUtil.escaparLike(termino), idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.superBuscador(BusquedaUtil.escaparLike(termino), idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona) {
        return listarPorCliente(idCliente, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.findByClienteIdAndPersonaYFirma(
                    idCliente, idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.findByClienteIdAndPersona(idCliente, idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona) {
        return buscarPorNombreOIdentificacionCliente(termino, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        if (contextoSeguridad.esAdmin()) {
            return aResponse(casoRepository.buscarPorClienteNombreOIdentificacionYFirma(
                    BusquedaUtil.escaparLike(termino), idPersona, contextoSeguridad.idFirmaAutenticadaRequerida(), Paginacion.crear(page, size)));
        }
        return aResponse(casoRepository.buscarPorClienteNombreOIdentificacion(BusquedaUtil.escaparLike(termino), idPersona, Paginacion.crear(page, size)));
    }

    @Override
    public CasoResponseDTO buscarPorId(Integer id) {
        // M25: verificarAccesoACaso ya rechaza el caso raíz.
        contextoSeguridad.verificarAccesoACaso(id);
        return casoRepository.findById(id)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
    }

    @Override
    @Transactional
    public void desactivarCaso(Integer id) {
        Validaciones.requerirId(id, "id");
        // M25: verificarAccesoACaso ya rechaza el caso raíz.
        contextoSeguridad.verificarAccesoACaso(id);

        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        caso.setActivo(false);
        casoRepository.save(caso);
        // B3: la desactivación queda registrada en el historial, con el
        // mismo patrón que actualizar().
        registrarCambioEnHistorial(caso, contextoSeguridad.idPersonaAutenticada(),
                "Desactivación del caso.");
        // S4: la desactivación es solo el flag 'activo'. No hay borrado físico
        // de casos en ningún servicio (casoRepository.delete no se usa): el
        // cascade ALL de Caso.subCasos nunca se dispara desde esta capa, y el
        // trigger V3 de borrado lógico solo actúa sobre DELETE físico, que la
        // API no emite. La desactivación no se propaga a los subcasos: cada
        // subcaso se desactiva por separado, por diseño.
    }

    @Override
    @Transactional
    public void reactivarCaso(Integer id) {
        Validaciones.requerirId(id, "id");
        // Solo administradores de la firma (M9/B3). verificarAccesoACaso
        // impide además reactivar casos de otra firma y el caso raíz.
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta operación.");
        }
        contextoSeguridad.verificarAccesoACaso(id);

        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        caso.setActivo(true);
        casoRepository.save(caso);
        // Solo se reactiva el caso: nada más (ni subcasos ni vínculos).
        registrarCambioEnHistorial(caso, contextoSeguridad.idPersonaAutenticada(),
                "Reactivación del caso.");
    }

    /**
     * Crea el vínculo 'Principal' del caso recién radicado, en estado
     * AUTORIZADO (el creador autoriza su propio vínculo al radicar).
     * Migración V4: el antiguo flag 'autorizado' ya no existe; la fuente
     * de verdad es {@link Vinculo_Caso#getEstado()}.
     */
    private void crearVinculo(Caso caso, Integer idPersona, Cliente cliente) {
        // C6: el cliente debe estar activo y pertenecer a la firma del caso.
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente no está activo.");
        }
        if (cliente.getFirma() == null || caso.getFirma() == null
                || !cliente.getFirma().getId().equals(caso.getFirma().getId())) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        Persona persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // El vínculo 'Principal' se busca por nombre, no por id mágico: en
        // una base nueva solo existe 'SIN VÍNCULO' y el id 2 no está garantizado.
        Vinculo tipoVinculo = vinculoRepository.findByNombre("Principal")
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        Vinculo_Caso vinculoCaso = new Vinculo_Caso();
        vinculoCaso.setCaso(caso);
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setVinculo(tipoVinculo);
        vinculoCaso.setEstado(EstadoVinculo.AUTORIZADO);

        vinculoCasoRepository.save(vinculoCaso);
    }

    private void registrarCambioEnHistorial(Caso caso, Integer idPersona, String descripcion) {
        Persona persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        Historial_Cambio historial = new Historial_Cambio();
        historial.setCaso(caso);
        historial.setPersona(persona);
        historial.setDescripcion(descripcion);
        historial.setFecha(LocalDate.now(ZONA_BOGOTA));
        historialCambioRepository.save(historial);
    }

    /** Verdadero si el estado pertenece al conjunto terminal (A4). */
    private boolean esTerminal(Estado estado) {
        return estado != null && estado.getNombre() != null
                && ESTADOS_TERMINALES.contains(estado.getNombre().toUpperCase(Locale.ROOT));
    }

    /**
     * M2: convierte el resultado de un listado a DTOs, excluyendo el caso
     * raíz del sistema (id 1), que nunca se lista. Sustituye al antiguo
     * post-filtro en memoria: el filtrado por firma ya lo hace la consulta
     * JPQL en la rama admin.
     */
    private List<CasoResponseDTO> aResponse(List<Caso> casos) {
        return casos.stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
