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
import com.example.lexcloud.service.AgentaService;
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

    public AgendaServiceImpl(Caso_Repository casoRepository,
                             Abogado_Repository abogadoRepository,
                             TipoAgenda_Repository tipoAgendaRepository,
                             Agenda_Repository agendaRepository,
                             AgendaMapper agendaMapper) {
        this.casoRepository = casoRepository;
        this.abogadoRepository = abogadoRepository;
        this.tipoAgendaRepository = tipoAgendaRepository;
        this.agendaRepository = agendaRepository;
        this.agendaMapper = agendaMapper;
    }

    @Override
    public AgendaResponseDTO guardar(AgendaRequestDTO request) {
        // VALIDACIÓN ESPECIAL: El caso 1 es del sistema y no permite agenda
        if (request.idCaso() != null && request.idCaso() == 1) {
            throw new EntityNotFoundException("Caso no Encontrado");
        }

        Agenda agenda = agendaMapper.toEntity(request);

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
        List<Agenda> agendas = agendaRepository.findByAbogadoId(idAbogado);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCaso(Integer idCaso) {
        if (idCaso == 1) {
            return List.of(); // Devuelve lista vacía inmediatamente
        }

        List<Agenda> agendas = agendaRepository.findByCasoId(idCaso);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public  List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin) {
        List<Agenda> agendas = agendaRepository.findAgendaByAbogadoAndFechaRange(idAbogado, inicio, fin);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin) {
        List<Agenda> agendas = agendaRepository.findByCasoIdAndFechaRange(idCaso, inicio, fin);

        return agendaMapper.toResponseList(agendas);
    }

    @Override
    public List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado) {
        List<Agenda> agendas = agendaRepository.findProximasCitasByAbogado(idAbogado);

        return  agendaMapper.toResponseList(agendas);
    }

    @Override
    public Optional<AgendaResponseDTO> buscarPorId(Integer id) {
        return agendaRepository.findById(id)
                .map(agendaMapper::toResponse);
    }

    @Override
    public  AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request) {
        // VALIDACIÓN ESPECIAL: El caso 1 es del sistema y no permite agenda
        if (request.idCaso() != null && request.idCaso() == 1) {
            throw new EntityNotFoundException("Caso no Encontrado");
        }

        return agendaRepository.findById(id).map( oldAgenda -> {
            Abogado nuevoAbogado = abogadoRepository.findById(request.idAbogado())
                    .orElseThrow(() -> new EntityNotFoundException("Abogado no Encontrado"));
            Caso nuevoCaso = casoRepository.findById(request.idCaso())
                    .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
            Tipo_Agenda nuevoTipoAgenda = tipoAgendaRepository.findById(request.idTipoAgenda())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo Agenda no encontrado"));

            oldAgenda.setAbogado(nuevoAbogado);
            oldAgenda.setCaso(nuevoCaso);
            oldAgenda.setTipo_Agenda(nuevoTipoAgenda);

            return agendaMapper.toResponse(agendaRepository.save(oldAgenda));
        }).orElseThrow(() -> new EntityNotFoundException("No se encontro la Agenda: " + id));
    }

    @Override
    public void desactivarAgenda(Integer id){
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agenda no encontrado"));

        agenda.setActiva(false);

        agendaRepository.save(agenda);
    }
}
