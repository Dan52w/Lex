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
import com.example.lexcloud.service.AgentaService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AgendaServiceImpl implements AgentaService {
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
    public AgendaResponseDTO guardar(AgendaRequestDTO request) {
        // VALIDACIÓN ESPECIAL: El caso 1 es del sistema y no permite agenda
        if (request.idCaso() != null && request.idCaso() == 1) {
            throw new EntityNotFoundException("Caso no Encontrado");
        }
        // SVC-4: solo se agenda sobre casos a los que se tiene acceso.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        Agenda agenda = agendaMapper.toEntity(request);

        // Control de valores predefinidos si los opcionales del DTO vienen nulos
        if (agenda.getActiva() == null) {
            agenda.setActiva(true);
        }
        if (agenda.getDescripcion() == null) {
            agenda.setDescripcion("");
        }

        agenda.setAbogado(abogadoRepository.findById(request.idAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Abogado no Encontrado")));
        agenda.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no Encontrado")));
        agenda.setTipo_Agenda(tipoAgendaRepository.findById(request.idTipoAgenda())
                .orElseThrow(() -> new EntityNotFoundException("Tipo Agenda no encontrado")));

        return agendaMapper.toResponse(agendaRepository.save(agenda));
    }

    @Override
    public List<AgendaResponseDTO> listarAbogado(Integer idAbogado) {
        return listarAbogado(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarAbogado(Integer idAbogado, int page, int size) {
        verificarAccesoAAbogadoPorId(idAbogado);
        List<Agenda> agendas = agendaRepository.findByAbogadoId(idAbogado, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCaso(Integer idCaso) {
        return listarCaso(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarCaso(Integer idCaso, int page, int size) {
        if (idCaso == 1) {
            return List.of(); // Devuelve lista vacía inmediatamente
        }
        contextoSeguridad.verificarAccesoACaso(idCaso);

        List<Agenda> agendas = agendaRepository.findByCasoId(idCaso, Paginacion.crear(page, size));

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public  List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin) {
        verificarAccesoAAbogadoPorId(idAbogado);
        // Sin variante paginada en el repositorio para el rango de fechas.
        List<Agenda> agendas = agendaRepository.findAgendaByAbogadoAndFechaRange(idAbogado, inicio, fin);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin) {
        if (idCaso == 1) {
            return List.of();
        }
        contextoSeguridad.verificarAccesoACaso(idCaso);
        List<Agenda> agendas = agendaRepository.findByCasoIdAndFechaRange(idCaso, inicio, fin);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado) {
        return listarProximaCitas(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado, int page, int size) {
        verificarAccesoAAbogadoPorId(idAbogado);
        List<Agenda> agendas = agendaRepository.findProximasCitasByAbogado(idAbogado, Paginacion.crear(page, size));

        return  agendaMapper.toResponseList(agendas);
    }

    @Override
    public Optional<AgendaResponseDTO> buscarPorId(Integer id) {
        return agendaRepository.findById(id)
                .map(agenda -> {
                    contextoSeguridad.verificarAccesoACaso(agenda.getCaso().getId());
                    return agendaMapper.toResponse(agenda);
                });
    }

    @Override
    public  AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request) {
        // VALIDACIÓN ESPECIAL: El caso 1 es del sistema y no permite agenda
        if (request.idCaso() != null && request.idCaso() == 1) {
            throw new EntityNotFoundException("Caso no Encontrado");
        }

        return agendaRepository.findById(id).map( oldAgenda -> {
            // Solo se puede modificar una agenda de un caso al que se tiene acceso,
            // y el caso destino también debe ser accesible.
            contextoSeguridad.verificarAccesoACaso(oldAgenda.getCaso().getId());
            contextoSeguridad.verificarAccesoACaso(request.idCaso());

            Abogado nuevoAbogado = abogadoRepository.findById(request.idAbogado())
                    .orElseThrow(() -> new EntityNotFoundException("Abogado no Encontrado"));
            Caso nuevoCaso = casoRepository.findById(request.idCaso())
                    .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
            Tipo_Agenda nuevoTipoAgenda = tipoAgendaRepository.findById(request.idTipoAgenda())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo Agenda no encontrado"));

            oldAgenda.setAbogado(nuevoAbogado);
            oldAgenda.setCaso(nuevoCaso);
            oldAgenda.setTipo_Agenda(nuevoTipoAgenda);
            // Solo se sobrescriben los opcionales cuando vienen con valor
            if (request.descripcion() != null) {
                oldAgenda.setDescripcion(request.descripcion());
            }
            if (request.fechaExp() != null) {
                oldAgenda.setFechaExp(request.fechaExp());
            }
            if (request.activa() != null) {
                oldAgenda.setActiva(request.activa());
            }

            return agendaMapper.toResponse(agendaRepository.save(oldAgenda));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontro la Agenda: " + id));
    }

    @Override
    public void desactivarAgenda(Integer id){
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agenda no encontrado"));

        contextoSeguridad.verificarAccesoACaso(agenda.getCaso().getId());
        agenda.setActiva(false);

        agendaRepository.save(agenda);
    }

    private void verificarAccesoAAbogadoPorId(Integer idAbogado) {
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no Encontrado"));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
    }
}
