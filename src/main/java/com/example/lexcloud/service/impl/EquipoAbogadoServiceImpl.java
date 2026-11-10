package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.EquipoAbogadoMapper;
import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Equipo_Abogados;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.EquipoAbogado_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.TipoRelacionEquipo_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.EquipoAbogadoService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoAbogadoServiceImpl implements EquipoAbogadoService {

    private final EquipoAbogado_Repository equipoAbogadoRepository;
    private final Abogado_Repository abogadoRepository;
    private final Persona_Repository personaRepository;
    private final TipoRelacionEquipo_Repository tipoRelacionRepository;
    private final EquipoAbogadoMapper equipoAbogadoMapper;
    private final ContextoSeguridad contextoSeguridad;

    public EquipoAbogadoServiceImpl(EquipoAbogado_Repository equipoAbogadoRepository,
                                    Abogado_Repository abogadoRepository,
                                    Persona_Repository personaRepository,
                                    TipoRelacionEquipo_Repository tipoRelacionRepository,
                                    EquipoAbogadoMapper equipoAbogadoMapper,
                                    ContextoSeguridad contextoSeguridad) {
        this.equipoAbogadoRepository = equipoAbogadoRepository;
        this.abogadoRepository = abogadoRepository;
        this.personaRepository = personaRepository;
        this.tipoRelacionRepository = tipoRelacionRepository;
        this.equipoAbogadoMapper = equipoAbogadoMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public EquipoAbogadoResponseDTO crearAsignacion(EquipoAbogadoRequestDTO requestDTO) {
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

        var persona = personaRepository.findById(requestDTO.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Miembro del personal (Persona) no encontrado con el ID: " + requestDTO.idPersona()));

        var tipoRelacion = tipoRelacionRepository.findById(requestDTO.idTipoRelacion())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de relación no encontrado con el ID: " + requestDTO.idTipoRelacion()));

        // 2b. La asignación se crea en la firma del abogado: quien crea debe
        // pertenecer a esa firma. (La persona asignada no se puede verificar por
        // firma: el modelo no guarda la firma en Persona, solo en Abogado.)
        contextoSeguridad.verificarAccesoAAbogado(abogado);

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
        contextoSeguridad.verificarAccesoAAbogado(ea.getAbogado());
        return equipoAbogadoMapper.toResponse(ea);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarTodosActivos() {
        return listarTodosActivos(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarTodosActivos(int page, int size) {
        // SVC-2: la variante global (todas las firmas) es solo para administradores;
        // el resto ve únicamente las asignaciones activas de su propia firma.
        if (contextoSeguridad.esAdmin()) {
            return equipoAbogadoMapper.toResponseList(
                    equipoAbogadoRepository.findAllActiveWithRelationships(Paginacion.crear(page, size)));
        }
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findActiveWithRelationshipsByFirma(firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado) {
        var abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado con el ID: " + idAbogado));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        return equipoAbogadoMapper.toResponseList(equipoAbogadoRepository.findByAbogado_Id(idAbogado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado) {
        var abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado con el ID: " + idAbogado));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        return equipoAbogadoMapper.toResponseList(equipoAbogadoRepository.findByAbogado_IdAndActivoTrue(idAbogado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona) {
        // Sin variante paginada ni con alcance por firma en el repositorio para
        // esta consulta: se filtra en memoria a la firma del usuario (salvo admin).
        List<Equipo_Abogados> asignaciones = equipoAbogadoRepository.findByPersona_Id(idPersona);
        return equipoAbogadoMapper.toResponseList(filtrarPorFirma(asignaciones));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion) {
        List<Equipo_Abogados> asignaciones = equipoAbogadoRepository.findByTipoRelacion_Id(idTipoRelacion);
        return equipoAbogadoMapper.toResponseList(filtrarPorFirma(asignaciones));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion) {
        // SVC-2: solo administradores ven las activas de todas las firmas.
        if (contextoSeguridad.esAdmin()) {
            return equipoAbogadoMapper.toResponseList(
                    equipoAbogadoRepository.findActiveByRelationshipType(idTipoRelacion));
        }
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findActiveByRelationshipTypeAndFirma(idTipoRelacion, firmaId));
    }

    @Override
    @Transactional
    public void desactivarAsignacion(Integer idAbogado, Integer idPersona) {
        Equipo_Abogados asignacion = equipoAbogadoRepository.findByAbogado_IdAndPersona_IdAndActivoTrue(idAbogado, idPersona)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una asignación activa para los IDs provistos."));

        contextoSeguridad.verificarAccesoAAbogado(asignacion.getAbogado());
        asignacion.setActivo(false); // Desactivación lógica segura
        equipoAbogadoRepository.save(asignacion);
    }

    @Override
    @Transactional
    public void eliminarAsignacion(Integer id) {
        Equipo_Abogados asignacion = equipoAbogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar. No existe registro con el ID: " + id));
        contextoSeguridad.verificarAccesoAAbogado(asignacion.getAbogado());
        equipoAbogadoRepository.delete(asignacion);
    }

    /**
     * Filtra asignaciones a la firma del usuario autenticado (sin filtro para
     * administradores). Si la firma no se puede resolver, se deniega: devolver
     * la lista sin filtrar expondría asignaciones de todas las firmas.
     */
    private List<Equipo_Abogados> filtrarPorFirma(List<Equipo_Abogados> asignaciones) {
        if (contextoSeguridad.esAdmin()) {
            return asignaciones;
        }
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return asignaciones.stream()
                .filter(ea -> ea.getAbogado() != null
                        && ea.getAbogado().getFirma() != null
                        && firmaId.equals(ea.getAbogado().getFirma().getId()))
                .collect(Collectors.toList());
    }
}
