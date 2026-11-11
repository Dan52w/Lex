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
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipoAbogadoServiceImpl implements EquipoAbogadoService {

    /** Zona horaria para las fechas que fija el servidor. */
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Bogota");

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
        // M7: guardas de null antes de cualquier acceso a repositorio.
        Validaciones.requerirNoNulo(requestDTO, "request");
        Integer idAbogado = Validaciones.requerirId(requestDTO.idAbogado(), "idAbogado");
        Integer idPersona = Validaciones.requerirId(requestDTO.idPersona(), "idPersona");
        Integer idTipoRelacion = Validaciones.requerirId(requestDTO.idTipoRelacion(), "idTipoRelacion");

        // M20: primero se cargan las entidades (404 genérico, sin IDs — B2).
        // El chequeo de duplicado va DESPUÉS de cargarlas, para no oracular
        // la existencia del par abogado/persona antes de verificar nada.
        var abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        var persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        var tipoRelacion = tipoRelacionRepository.findById(idTipoRelacion)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        // 2b. La asignación se crea en la firma del abogado: quien crea debe
        // pertenecer a esa firma. (La persona asignada no se puede verificar por
        // firma: el modelo no guarda la firma en Persona, solo en Abogado.)
        contextoSeguridad.verificarAccesoAAbogado(abogado);

        // M20: estado y coherencia de la asignación.
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("La persona no está activa.");
        }
        if (contextoSeguridad.esPersonaSistema(persona)) {
            throw new IllegalArgumentException("La persona del sistema no puede formar parte de un equipo.");
        }
        if (!Boolean.TRUE.equals(abogado.getActivo())
                || abogado.getPersona() == null
                || !Boolean.TRUE.equals(abogado.getPersona().getActivo())) {
            throw new IllegalArgumentException("El abogado no está activo.");
        }
        // Auto-asignación: un abogado no puede asignarse a sí mismo como
        // miembro de su propio equipo.
        if (abogado.getPersona().getId().equals(persona.getId())) {
            throw new IllegalArgumentException("Un abogado no puede asignarse a sí mismo como miembro de su propio equipo.");
        }
        if (requestDTO.fechaAsignacion() != null
                && requestDTO.fechaAsignacion().isAfter(LocalDate.now(ZONA_HORARIA))) {
            throw new IllegalArgumentException("La fecha de asignación no puede ser futura.");
        }

        // M20: la regla de no duplicar asignaciones activas idénticas se
        // comprueba ahora, con las entidades ya cargadas y validadas.
        boolean existeActiva = equipoAbogadoRepository.existsByAbogado_IdAndPersona_IdAndActivoTrue(
                idAbogado, idPersona
        );
        if (existeActiva) {
            throw new IllegalStateException("Ya existe una relación activa vigente entre este abogado y el miembro del personal.");
        }

        // 3. Mapear de Request DTO a Entidad usando MapStruct
        Equipo_Abogados nuevaAsignacion = equipoAbogadoMapper.toEntity(requestDTO);

        // 4. Inyectar las relaciones completas para evitar fallos de persistencia (Constraint Violations)
        nuevaAsignacion.setAbogado(abogado);
        nuevaAsignacion.setPersona(persona);
        nuevaAsignacion.setTipoRelacion(tipoRelacion);

        // 5. Control de valores predefinidos si los parámetros opcionales del DTO vienen nulos
        if (nuevaAsignacion.getFechaAsignacion() == null) {
            nuevaAsignacion.setFechaAsignacion(LocalDate.now(ZONA_HORARIA));
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
        Integer idAsignacion = Validaciones.requerirId(id, "id");
        Equipo_Abogados ea = equipoAbogadoRepository.findById(idAsignacion)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
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
        // C8: ya no hay rama global para administradores: TODOS ven solo las
        // asignaciones activas de su propia firma (la variante con firma del
        // repositorio, H3). Un admin de la firma A ya no ve datos de la B.
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findAllActiveWithRelationshipsByFirma(firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado) {
        return listarPorAbogado(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado, int page, int size) {
        Integer id = Validaciones.requerirId(idAbogado, "idAbogado");
        var abogado = abogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findByAbogado_Id(id, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado) {
        return listarActivosPorAbogado(idAbogado, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado, int page, int size) {
        Integer id = Validaciones.requerirId(idAbogado, "idAbogado");
        var abogado = abogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findByAbogado_IdAndActivoTrue(id, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona) {
        return listarPorPersona(idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona, int page, int size) {
        Integer id = Validaciones.requerirId(idPersona, "idPersona");
        // M21: solo la propia persona o un administrador pueden consultar
        // las asignaciones de una persona.
        contextoSeguridad.verificarMismaPersona(id);
        // C8/H3: alcance por firma en JPQL (variante con firma y paginada),
        // no filtrado en memoria y nunca la variante global.
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findByPersonaIdAndFirma(id, firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion) {
        return listarPorTipoRelacion(idTipoRelacion, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion, int page, int size) {
        Integer id = Validaciones.requerirId(idTipoRelacion, "idTipoRelacion");
        // C8/H3: alcance por firma en JPQL, nunca la variante global con
        // filtrado en memoria.
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findByTipoRelacionIdAndFirma(id, firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion) {
        return listarActivosPorTipoRelacion(idTipoRelacion, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion, int page, int size) {
        Integer id = Validaciones.requerirId(idTipoRelacion, "idTipoRelacion");
        // C8/H3: la antigua rama admin llamaba a findActiveByRelationshipType,
        // variante global que el Lote 2 borró del repositorio (el código ya no
        // compilaba). Ahora TODOS usan la variante con firma y paginada.
        Integer firmaId = contextoSeguridad.idFirmaAutenticadaRequerida();
        return equipoAbogadoMapper.toResponseList(
                equipoAbogadoRepository.findActiveByRelationshipTypeAndFirma(id, firmaId, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional
    public void desactivarAsignacion(Integer idAbogado, Integer idPersona) {
        Integer idAb = Validaciones.requerirId(idAbogado, "idAbogado");
        Integer idPe = Validaciones.requerirId(idPersona, "idPersona");
        Equipo_Abogados asignacion = equipoAbogadoRepository.findByAbogado_IdAndPersona_IdAndActivoTrue(idAb, idPe)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una asignación activa."));

        contextoSeguridad.verificarAccesoAAbogado(asignacion.getAbogado());
        asignacion.setActivo(false); // Desactivación lógica segura
        equipoAbogadoRepository.save(asignacion);
    }

    @Override
    @Transactional
    public void eliminarAsignacion(Integer id) {
        Integer idAsignacion = Validaciones.requerirId(id, "id");
        Equipo_Abogados asignacion = equipoAbogadoRepository.findById(idAsignacion)
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        contextoSeguridad.verificarAccesoAAbogado(asignacion.getAbogado());
        equipoAbogadoRepository.delete(asignacion);
    }
}
