package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EquipoAbogadoMapper;
import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Equipo_Abogados;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.EquipoAbogado_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.TipoRelacionEquipo_Repository;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.EquipoAbogadoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EquipoAbogadoServiceImpl implements EquipoAbogadoService {

    private final EquipoAbogado_Repository equipoAbogadoRepository;
    private final Abogado_Repository abogadoRepository;
    private final Persona_Repository personaRepository;
    private final TipoRelacionEquipo_Repository tipoRelacionRepository;
    private final EquipoAbogadoMapper equipoAbogadoMapper;
    private final CurrentUser currentUser;

    public EquipoAbogadoServiceImpl(EquipoAbogado_Repository equipoAbogadoRepository,
                                    Abogado_Repository abogadoRepository,
                                    Persona_Repository personaRepository,
                                    TipoRelacionEquipo_Repository tipoRelacionRepository,
                                    EquipoAbogadoMapper equipoAbogadoMapper,
                                    CurrentUser currentUser) {
        this.equipoAbogadoRepository = equipoAbogadoRepository;
        this.abogadoRepository = abogadoRepository;
        this.personaRepository = personaRepository;
        this.tipoRelacionRepository = tipoRelacionRepository;
        this.equipoAbogadoMapper = equipoAbogadoMapper;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public EquipoAbogadoResponseDTO crearAsignacion(EquipoAbogadoRequestDTO requestDTO) {
        // P0-B3: el alta de miembros/equipo es solo-admin.
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        // 1. Validar regla de negocio: No duplicar asignaciones activas idénticas
        boolean existeActiva = equipoAbogadoRepository.existsByAbogado_IdAndPersona_IdAndActivoTrue(
                requestDTO.idAbogado(), requestDTO.idPersona()
        );
        if (existeActiva) {
            throw new IllegalStateException("Ya existe una relación activa vigente entre este abogado y el miembro del personal.");
        }

        // 2. Buscar las entidades relacionales correspondientes en la base de datos
        var abogado = abogadoRepository.findById(requestDTO.idAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado con el ID: " + requestDTO.idAbogado()));
        // P0-B2: el abogado debe ser de la firma del token (o admin).
        if (abogado.getFirma() != null) {
            currentUser.requireFirma(abogado.getFirma().getId());
        }

        var persona = personaRepository.findById(requestDTO.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Miembro del personal (Persona) no encontrado con el ID: " + requestDTO.idPersona()));

        var tipoRelacion = tipoRelacionRepository.findById(requestDTO.idTipoRelacion())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de relación no encontrado con el ID: " + requestDTO.idTipoRelacion()));

        // 3. Mapear de Request DTO a Entidad usando MapStruct
        Equipo_Abogados nuevaAsignacion = equipoAbogadoMapper.toEntity(requestDTO);

        // 4. Inyectar las relaciones completas para evitar fallos de persistencia (Constraint Violations)
        nuevaAsignacion.setAbogado(abogado);
        nuevaAsignacion.setPersona(persona);
        nuevaAsignacion.setTipoRelacion(tipoRelacion);

        // 5. Control de valores predefinidos si los parámetros opcionales del DTO vienen nulos
        if (nuevaAsignacion.getFechaAsignacion() == null) {
            nuevaAsignacion.setFechaAsignacion(LocalDate.now());
        }
        if (nuevaAsignacion.getActivo() == null) {
            nuevaAsignacion.setActivo(true);
        }

        Equipo_Abogados guardado = equipoAbogadoRepository.save(nuevaAsignacion);
        return equipoAbogadoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoAbogadoResponseDTO obtenerPorId(Integer id) {
        Equipo_Abogados ea = equipoAbogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Asignación de equipo no encontrada con el ID: " + id));
        if (ea.getAbogado() != null && ea.getAbogado().getFirma() != null) {
            currentUser.requireFirma(ea.getAbogado().getFirma().getId());
        }
        return equipoAbogadoMapper.toResponse(ea);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarTodosActivos() {
        // P0-B2: un no-admin solo ve asignaciones de su firma (filtro en
        // memoria porque el repositorio no filtra por firma; es P1-B2
        // paginar/filtrar en BD).
        List<com.example.lexcloud.entidades.Equipo_Abogados> todas =
                equipoAbogadoRepository.findAllActiveWithRelationships();
        if (!currentUser.isAdmin()) {
            Integer firmaToken = currentUser.requireFirmaId();
            todas = todas.stream()
                    .filter(ea -> ea.getAbogado() != null && ea.getAbogado().getFirma() != null
                            && ea.getAbogado().getFirma().getId().equals(firmaToken))
                    .toList();
        }
        return equipoAbogadoMapper.toResponseList(todas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado) {
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));
        if (abogado.getFirma() != null) {
            currentUser.requireFirma(abogado.getFirma().getId());
        }
        return equipoAbogadoMapper.toResponseList(equipoAbogadoRepository.findByAbogado_Id(idAbogado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado) {
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));
        if (abogado.getFirma() != null) {
            currentUser.requireFirma(abogado.getFirma().getId());
        }
        return equipoAbogadoMapper.toResponseList(equipoAbogadoRepository.findByAbogado_IdAndActivoTrue(idAbogado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona) {
        // P0-B2: uno mismo, admin, o misma firma del token.
        if (!currentUser.isAdmin() && !currentUser.personaId().equals(idPersona)) {
            Integer firmaObjetivo = currentUser.firmaOfPersona(idPersona);
            if (firmaObjetivo == null || !currentUser.canAccessFirma(firmaObjetivo)) {
                throw new SecurityException("No tiene permisos para realizar esta acción");
            }
        }
        return equipoAbogadoMapper.toResponseList(equipoAbogadoRepository.findByPersona_Id(idPersona));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion) {
        List<com.example.lexcloud.entidades.Equipo_Abogados> todas =
                equipoAbogadoRepository.findByTipoRelacion_Id(idTipoRelacion);
        if (!currentUser.isAdmin()) {
            Integer firmaToken = currentUser.requireFirmaId();
            todas = todas.stream()
                    .filter(ea -> ea.getAbogado() != null && ea.getAbogado().getFirma() != null
                            && ea.getAbogado().getFirma().getId().equals(firmaToken))
                    .toList();
        }
        return equipoAbogadoMapper.toResponseList(todas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion) {
        List<com.example.lexcloud.entidades.Equipo_Abogados> todas =
                equipoAbogadoRepository.findActiveByRelationshipType(idTipoRelacion);
        if (!currentUser.isAdmin()) {
            Integer firmaToken = currentUser.requireFirmaId();
            todas = todas.stream()
                    .filter(ea -> ea.getAbogado() != null && ea.getAbogado().getFirma() != null
                            && ea.getAbogado().getFirma().getId().equals(firmaToken))
                    .toList();
        }
        return equipoAbogadoMapper.toResponseList(todas);
    }

    @Override
    @Transactional
    public void desactivarAsignacion(Integer idAbogado, Integer idPersona) {
        Equipo_Abogados asignacion = equipoAbogadoRepository.findByAbogado_IdAndPersona_IdAndActivoTrue(idAbogado, idPersona)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una asignación activa para los IDs provistos."));
        if (asignacion.getAbogado() != null && asignacion.getAbogado().getFirma() != null) {
            currentUser.requireFirma(asignacion.getAbogado().getFirma().getId());
        }
        asignacion.setActivo(false); // Desactivación lógica segura
        equipoAbogadoRepository.save(asignacion);
    }

    @Override
    @Transactional
    public void eliminarAsignacion(Integer id) {
        Equipo_Abogados ea = equipoAbogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar. No existe registro con el ID: " + id));
        if (ea.getAbogado() != null && ea.getAbogado().getFirma() != null) {
            currentUser.requireFirma(ea.getAbogado().getFirma().getId());
        }
        equipoAbogadoRepository.deleteById(id);
    }
}