package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.AgendaMapper;
import com.example.lexcloud.dto.request.AgendaRequestDTO;
import com.example.lexcloud.dto.response.AgendaResponseDTO;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Agenda;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Tipo_Agenda;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Agenda_Repository;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.TipoAgenda_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AgendaService;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AgendaServiceImpl implements AgendaService {

    /** Zona horaria de operación (B6). */
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final Caso_Repository casoRepository;
    private final Abogado_Repository abogadoRepository;
    private final TipoAgenda_Repository tipoAgendaRepository;
    private final Agenda_Repository agendaRepository;
    private final AgendaMapper agendaMapper;
    private final ContextoSeguridad contextoSeguridad;

    public AgendaServiceImpl(Caso_Repository casoRepository,
                             Abogado_Repository abogadoRepository,
                             TipoAgenda_Repository tipoAgendaRepository,
                             Agenda_Repository agendaRepository,
                             AgendaMapper agendaMapper,
                             ContextoSeguridad contextoSeguridad) {
        this.casoRepository = casoRepository;
        this.abogadoRepository = abogadoRepository;
        this.tipoAgendaRepository = tipoAgendaRepository;
        this.agendaRepository = agendaRepository;
        this.agendaMapper = agendaMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public AgendaResponseDTO guardar(AgendaRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        Validaciones.requerirId(request.idCaso(), "idCaso");
        Validaciones.requerirId(request.idAbogado(), "idAbogado");
        Validaciones.requerirId(request.idTipoAgenda(), "idTipoAgenda");

        // M25: verificarAccesoACaso ya rechaza el caso raíz.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());
        // A7: la fecha debe ser futura, también en el servicio (además del
        // @Future del DTO, que solo actúa si el controlador valida).
        validarFechaFutura(request.fechaExp());

        Agenda agenda = agendaMapper.toEntity(request);

        // B6: descripción null en guardar -> "" (la columna es NOT NULL);
        // en actualizar null conserva el valor actual (ver actualizar()).
        if (agenda.getActiva() == null) {
            agenda.setActiva(true);
        }
        if (agenda.getDescripcion() == null) {
            agenda.setDescripcion("");
        }

        Abogado abogado = abogadoRepository.findById(request.idAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // A1: acceso por el helper central + abogado y persona activos.
        verificarAbogadoActivo(abogado);
        agenda.setAbogado(abogado);

        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // A1: no se agenda sobre un caso inactivo.
        verificarCasoActivo(caso);
        agenda.setCaso(caso);

        agenda.setTipo_Agenda(tipoAgendaRepository.findById(request.idTipoAgenda())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado")));

        // A7: sin doble reserva del mismo abogado en la misma fecha/hora.
        verificarSinDobleReserva(abogado.getId(), agenda.getFechaExp(), null);

        return agendaMapper.toResponse(agendaRepository.save(agenda));
    }

    @Override
    public List<AgendaResponseDTO> listarAbogado(Integer idAbogado) {
        return listarAbogado(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarAbogado(Integer idAbogado, int page, int size) {
        contextoSeguridad.verificarAccesoAAbogadoPorId(idAbogado);
        List<Agenda> agendas = agendaRepository.findByAbogadoId(idAbogado, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCaso(Integer idCaso) {
        return listarCaso(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarCaso(Integer idCaso, int page, int size) {
        // M25: el caso raíz devuelve lista vacía (nunca tiene agendas).
        if (!tieneAccesoACasoParaListado(idCaso)) {
            return List.of();
        }
        List<Agenda> agendas = agendaRepository.findByCasoId(idCaso, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin) {
        return listarAbogadoFechaRange(idAbogado, inicio, fin, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin, int page, int size) {
        validarRango(inicio, fin);
        contextoSeguridad.verificarAccesoAAbogadoPorId(idAbogado);
        List<Agenda> agendas = agendaRepository.findAgendaByAbogadoAndFechaRange(idAbogado, inicio, fin, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin) {
        return listarCasoFechaRange(idCaso, inicio, fin, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin, int page, int size) {
        validarRango(inicio, fin);
        // M25: el caso raíz devuelve lista vacía (nunca tiene agendas).
        if (!tieneAccesoACasoParaListado(idCaso)) {
            return List.of();
        }
        List<Agenda> agendas = agendaRepository.findByCasoIdAndFechaRange(idCaso, inicio, fin, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado) {
        return listarProximaCitas(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado, int page, int size) {
        contextoSeguridad.verificarAccesoAAbogadoPorId(idAbogado);
        List<Agenda> agendas = agendaRepository.findProximasCitasByAbogado(idAbogado, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarMisAgendas(LocalDateTime inicio, LocalDateTime fin) {
        return listarMisAgendas(inicio, fin, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarMisAgendas(LocalDateTime inicio, LocalDateTime fin, int page, int size) {
        validarRango(inicio, fin);
        // V5 (dashboard): la agenda es del ALCANCE del usuario, no de un
        // abogado concreto. Antes el front resolvía el abogado por persona
        // (GET /api/abogados/persona/{id}) y el súper admin, el admin sin
        // perfil de abogado y la secretaria caían en 404: el dashboard
        // entero quedaba en estado de error aunque casos y clientes sí
        // cargaran. Ese endpoint por abogado se conserva; este es el
        // listado por alcance:
        // - Súper admin o admin bootstrap (sin firma resoluble): TODAS las
        //   firmas (idFirmaAutenticada() es vacío para ambos).
        // - Admin CON firma: todas las agendas activas de su firma.
        // - Abogado u otro rol CON firma: solo su propia agenda (perfil de
        //   abogado activo; sin perfil no hay agenda propia: lista vacía,
        //   nunca 403 ni 404).
        // - No-admin SIN firma resoluble: lista vacía (fail-closed, como
        //   ClienteServiceImpl.listarMiFirma).
        Optional<Integer> firma = contextoSeguridad.idFirmaAutenticada();
        if (firma.isPresent()) {
            if (contextoSeguridad.esAdmin()) {
                return agendaMapper.toResponseList(agendaRepository.findByFirmaEnRango(
                        firma.get(), inicio, fin, Paginacion.crear(page, size)));
            }
            return abogadoRepository.findByPersonaIdActivo(contextoSeguridad.idPersonaAutenticada())
                    .map(abogado -> agendaRepository.findAgendaByAbogadoAndFechaRange(
                            abogado.getId(), inicio, fin, Paginacion.crear(page, size)))
                    .map(agendaMapper::toResponseList)
                    .orElse(List.of());
        }
        if (contextoSeguridad.esAdmin()) {
            return agendaMapper.toResponseList(agendaRepository.findActivasEnRango(
                    inicio, fin, Paginacion.crear(page, size)));
        }
        return List.of();
    }

    @Override
    public Optional<AgendaResponseDTO> buscarPorId(Integer id) {
        return agendaRepository.findById(id)
                .map(agenda -> {
                    // M6: un agenda sin caso no puede verificarse: 404
                    // genérico en lugar de NPE.
                    if (agenda.getCaso() == null) {
                        throw new EntityNotFoundException("Recurso no encontrado");
                    }
                    contextoSeguridad.verificarAccesoACaso(agenda.getCaso().getId());
                    return agendaMapper.toResponse(agenda);
                });
    }

    @Override
    @Transactional
    public AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        Validaciones.requerirId(request.idCaso(), "idCaso");
        Validaciones.requerirId(request.idAbogado(), "idAbogado");
        Validaciones.requerirId(request.idTipoAgenda(), "idTipoAgenda");

        Agenda oldAgenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // M9 + A7: una agenda inactiva no se modifica ni se reactiva (no hay
        // flujo de reactivación de agendas).
        if (!Boolean.TRUE.equals(oldAgenda.getActiva())) {
            throw new IllegalArgumentException("No se puede modificar una agenda inactiva.");
        }

        // Solo se puede modificar una agenda de un caso al que se tiene acceso,
        // y el caso destino también debe ser accesible. M25: ambas llamadas
        // ya rechazan el caso raíz.
        if (oldAgenda.getCaso() == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        contextoSeguridad.verificarAccesoACaso(oldAgenda.getCaso().getId());
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        Abogado nuevoAbogado = abogadoRepository.findById(request.idAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // A1: acceso por el helper central + abogado y persona activos.
        verificarAbogadoActivo(nuevoAbogado);
        Caso nuevoCaso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // A1: no se mueve una agenda a un caso inactivo.
        verificarCasoActivo(nuevoCaso);
        Tipo_Agenda nuevoTipoAgenda = tipoAgendaRepository.findById(request.idTipoAgenda())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        oldAgenda.setAbogado(nuevoAbogado);
        oldAgenda.setCaso(nuevoCaso);
        oldAgenda.setTipo_Agenda(nuevoTipoAgenda);
        // Solo se sobrescriben los opcionales cuando vienen con valor.
        // B6: descripción null -> conserva el valor actual (en guardar,
        // en cambio, null -> "" porque la columna es NOT NULL).
        if (request.descripcion() != null) {
            oldAgenda.setDescripcion(request.descripcion());
        }
        if (request.fechaExp() != null) {
            // A7 + P4/P10: la nueva fecha debe ser futura solo si CAMBIÓ.
            // Sin este chequeo, editar solo la descripción de una agenda
            // vencida daba 400 (el @Future del DTO se quitó por lo mismo:
            // la regla vive aquí, no en el DTO).
            if (!request.fechaExp().equals(oldAgenda.getFechaExp())) {
                validarFechaFutura(request.fechaExp());
            }
            oldAgenda.setFechaExp(request.fechaExp());
        }
        if (request.activa() != null) {
            oldAgenda.setActiva(request.activa());
        }

        // A7: sin doble reserva del mismo abogado en la misma fecha/hora,
        // excluyendo la propia agenda del chequeo.
        verificarSinDobleReserva(
                nuevoAbogado.getId(), oldAgenda.getFechaExp(), oldAgenda.getId());

        return agendaMapper.toResponse(agendaRepository.save(oldAgenda));
    }

    @Override
    @Transactional
    public void desactivarAgenda(Integer id) {
        Validaciones.requerirId(id, "id");
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        if (agenda.getCaso() == null) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
        contextoSeguridad.verificarAccesoACaso(agenda.getCaso().getId());
        agenda.setActiva(false);

        agendaRepository.save(agenda);
    }

    /**
     * A1: acceso al abogado por el helper CENTRAL de ContextoSeguridad y
     * comprobación de activo (persona y registro de abogado). El helper
     * privado que hacía esto aquí se eliminó (M8).
     */
    private void verificarAbogadoActivo(Abogado abogado) {
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        if (abogado.getPersona() == null
                || !Boolean.TRUE.equals(abogado.getPersona().getActivo())
                || !Boolean.TRUE.equals(abogado.getActivo())) {
            throw new IllegalArgumentException("El abogado no está activo.");
        }
    }

    /** A1: no se agenda sobre un caso inactivo. */
    private void verificarCasoActivo(Caso caso) {
        if (!Boolean.TRUE.equals(caso.getActivo())) {
            throw new IllegalArgumentException("El caso no está activo.");
        }
    }

    /** A7: la fecha de la cita debe ser posterior al momento actual. */
    private void validarFechaFutura(LocalDateTime fechaExp) {
        if (fechaExp == null || !fechaExp.isAfter(LocalDateTime.now(ZONA_BOGOTA))) {
            throw new IllegalArgumentException("La fecha de la cita debe ser posterior a la actual.");
        }
    }

    /**
     * A7: rechaza la doble reserva (otra agenda ACTIVA del mismo abogado en
     * la misma fecha/hora). {@code idExcluir} excluye la propia agenda en
     * actualizar(); null en guardar().
     */
    private void verificarSinDobleReserva(Integer idAbogado, LocalDateTime fechaExp, Integer idExcluir) {
        boolean existe = (idExcluir == null)
                ? agendaRepository.existsByAbogado_IdAndFechaExpAndActivaTrue(idAbogado, fechaExp)
                : agendaRepository.existsByAbogado_IdAndFechaExpAndActivaTrueAndIdNot(idAbogado, fechaExp, idExcluir);
        if (existe) {
            throw new IllegalStateException("Ya existe una cita activa para ese abogado en esa fecha y hora.");
        }
    }

    /** M6: rango válido (inicio/fin no null, inicio <= fin). */
    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        Validaciones.requerirNoNulo(inicio, "inicio");
        Validaciones.requerirNoNulo(fin, "fin");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior o igual a la fecha de fin.");
        }
    }

    /**
     * M25: acceso a un caso para un LISTADO por caso. Devuelve false solo
     * cuando verificarAccesoACaso deniega con SecurityException, que en ese
     * método solo se lanza por el caso raíz (ver
     * ContextoSeguridad.verificarAccesoACaso): el listado lo traduce en
     * lista vacía en lugar de 403, sin guarda por id mágico en el
     * servicio. Un caso inexistente o sin acceso sigue dando 404 genérico
     * (EntityNotFoundException, no capturada aquí).
     */
    private boolean tieneAccesoACasoParaListado(Integer idCaso) {
        try {
            contextoSeguridad.verificarAccesoACaso(idCaso);
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }
}
