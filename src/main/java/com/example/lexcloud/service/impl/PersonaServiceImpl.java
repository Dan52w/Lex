package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.PersonaMapper;
import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.PersonaService;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PersonaServiceImpl implements PersonaService {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final Persona_Repository personaRepository;
    private final Rol_Repository rolRepository;
    private final PersonaMapper personaMapper;
    private final ContextoSeguridad contextoSeguridad;

    public PersonaServiceImpl(Persona_Repository personaRepository, Rol_Repository rolRepository,
                              PersonaMapper personaMapper, ContextoSeguridad contextoSeguridad) {
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.personaMapper = personaMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    /** C3: las escrituras de personas son solo para admin (salvo la excepción A12). */
    private void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede modificar personas.");
        }
    }

    @Override
    @Transactional
    public PersonaResponseDTO guardar(PersonaRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        String cc = Validaciones.requerirNoBlank(request.cc(), "cc").trim();
        Validaciones.requerirNoBlank(request.nombre(), "nombre");
        Validaciones.requerirNoBlank(request.apellido(), "apellido");
        Validaciones.requerirNoBlank(request.telefono(), "telefono");
        Validaciones.requerirId(request.idRol(), "idRol");
        validarFechaNacimiento(request.fechaNacimiento());
        // C3: crear personas es solo admin (A12 no abre aquí la excepción de
        // "misma persona": nadie puede auto-crearse con el rol que quiera).
        exigirAdmin();

        // 1. Validar unicidad de la CC
        if (personaRepository.findByCc(cc).isPresent()) {
            throw new IllegalStateException("Ya existe una persona registrada con esa cédula.");
        }

        PersonaRequestDTO normalizado = new PersonaRequestDTO(
                request.nombre().trim(), request.apellido().trim(), cc,
                request.idRol(), request.telefono().trim(), request.fechaNacimiento());
        Persona persona = personaMapper.toEntity(normalizado);

        persona.setRol(rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

        // Aseguramos que por defecto esté activo al crear
        persona.setActivo(true);

        try {
            return personaMapper.toResponse(personaRepository.save(persona));
        } catch (DataIntegrityViolationException ex) {
            // Defensa en profundidad (carrera entre dos peticiones con la misma
            // CC): re-consultar para distinguir el duplicado real de otra
            // violación de integridad, que se re-lanza con la causa.
            if (personaRepository.findByCc(cc).isPresent()) {
                throw new IllegalStateException("Ya existe una persona registrada con esa cédula.");
            }
            throw new IllegalStateException("No se pudo guardar la persona. Verifique los datos e intente nuevamente.", ex);
        }
    }

    @Override
    public Optional<PersonaResponseDTO> buscarPorCc(String cc) {
        // A12: PII por cédula — solo admin o la propia persona; cualquier
        // otro caso responde vacío (indistinguible de "no existe", sin oráculo).
        return personaRepository.findByCc(cc)
                .filter(this::visibleParaLlamador)
                .map(personaMapper::toResponse);
    }

    @Override
    public Optional<PersonaResponseDTO> buscarPorIdActivo(Integer id) {
        // Usamos el método que creamos anteriormente en el repositorio
        return personaRepository.findByIdAndActivoTrue(id)
                .filter(this::visibleParaLlamador)
                .map(personaMapper::toResponse);
    }

    @Override
    public Optional<PersonaResponseDTO> buscarPorId(Integer id) {
        return personaRepository.findById(id)
                .filter(this::visibleParaLlamador)
                .map(personaMapper::toResponse);
    }

    @Override
    public List<PersonaResponseDTO> listarTodo() {
        return listarTodo(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<PersonaResponseDTO> listarTodo(int page, int size) {
        // M1 + A12 (corrección del coordinador): listar TODAS las personas es
        // PII sensible; solo admin. Sin esta guarda, exponerlo por controlador
        // dejaría enumerar personas a cualquier usuario autenticado.
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("No tiene permisos para listar personas.");
        }
        // M1: no existía listado de personas; se añade con la variante
        // paginada de JpaRepository.
        return personaMapper.toResponseList(
                personaRepository.findAll(Paginacion.crear(page, size)).getContent());
    }

    @Override
    @Transactional
    public PersonaResponseDTO actualizar(Integer id, PersonaRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        Validaciones.requerirNoBlank(request.nombre(), "nombre");
        Validaciones.requerirNoBlank(request.apellido(), "apellido");
        Validaciones.requerirNoBlank(request.telefono(), "telefono");
        Validaciones.requerirId(request.idRol(), "idRol");
        validarFechaNacimiento(request.fechaNacimiento());
        // A12: solo admin o la propia persona.
        contextoSeguridad.verificarMismaPersona(id);

        Persona oldPersona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        vetarPersonaSistema(oldPersona);
        // M9 (mismo criterio que Cliente): una persona inactiva no se edita;
        // primero se reactiva con reactivar().
        if (Boolean.FALSE.equals(oldPersona.getActivo())) {
            throw new IllegalStateException("No se puede actualizar una persona inactiva.");
        }
        // C3: el rol solo lo cambia un admin — si no, un usuario podría
        // auto-asignarse el rol ADMINISTRADOR editando su propia persona.
        if ((oldPersona.getRol() == null && request.idRol() != null)
                || (oldPersona.getRol() != null && !oldPersona.getRol().getId().equals(request.idRol()))) {
            if (!contextoSeguridad.esAdmin()) {
                throw new SecurityException("Solo un administrador puede cambiar el rol de una persona.");
            }
        }
        oldPersona.setNombre(request.nombre().trim());
        oldPersona.setApellido(request.apellido().trim());
        //oldPersona.setCc(request.cc()); La cedula no deberia ser editable
        oldPersona.setTelefono(request.telefono().trim());
        oldPersona.setFechaNacimiento(request.fechaNacimiento());
        // El estado 'activo' NO se toca aquí: antes se forzaba a true en
        // cada actualización, lo que reactivaba en silencio a una persona
        // desactivada. La (des)activación es solo por desactivar()/reactivar().

        oldPersona.setRol(rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado")));

        return personaMapper.toResponse(personaRepository.save(oldPersona));
    }

    @Override
    public boolean existeCc(String cc) {
        // A12: oráculo de existencia por cédula. Un admin ve el valor real;
        // un no-admin solo obtiene true para su propia cédula; cualquier otra
        // cédula (exista o no) responde false, sin filtrar nada.
        if (cc == null) {
            return false;
        }
        if (contextoSeguridad.esAdmin()) {
            return personaRepository.existsByCc(cc);
        }
        return personaRepository.findByCc(cc)
                .map(persona -> persona.getId().equals(contextoSeguridad.idPersonaAutenticada()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Validaciones.requerirId(id, "id");
        // A12: solo admin o la propia persona (este es el "eliminar" de Persona).
        contextoSeguridad.verificarMismaPersona(id);
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));

        vetarPersonaSistema(persona);
        // Desactivar una persona NO desactiva su abogado: eso es competencia
        // de AbogadoServiceImpl (flag propio Abogado.activo). La persona
        // inactiva ya queda fuera de los listados de abogados por el filtro
        // persona.activo de sus consultas.
        persona.setActivo(false);
        personaRepository.save(persona);
    }

    @Override
    @Transactional
    public void reactivar(Integer id) {
        Validaciones.requerirId(id, "id");
        // B3 + C3: reactivar es solo admin (A12 no aplica aquí).
        exigirAdmin();
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        vetarPersonaSistema(persona);
        persona.setActivo(true);
        personaRepository.save(persona);
    }

    /** A12 (lecturas): admin o la propia persona; el resto ve vacío, sin oráculo. */
    private boolean visibleParaLlamador(Persona persona) {
        return contextoSeguridad.esAdmin()
                || persona.getId().equals(contextoSeguridad.idPersonaAutenticada());
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) {
        Validaciones.requerirNoNulo(fechaNacimiento, "fechaNacimiento");
        if (fechaNacimiento.isAfter(LocalDate.now(ZONA_BOGOTA))) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
    }

    /**
     * S1: la persona del sistema no se puede desactivar ni modificar.
     *
     * La entidad Persona tiene {@code @SQLDelete} (UPDATE activo=false en vez
     * de DELETE): si algún día se llamara a {@code personaRepository.delete()},
     * Hibernate emitiría el UPDATE y el trigger V3 de borrado de personas
     * (BEFORE DELETE, que registra el cambio y protege la fila) no se
     * dispararía nunca — la baja quedaría sin constancia. Por eso el único
     * camino de baja es este método, con save() explícito, y el borrado físico
     * de personas no existe en ningún servicio (verificado por grep: ningún
     * código llama a personaRepository.delete). La decisión de quitar o no el
     * {@code @SQLDelete} queda pendiente: no se puede probar aquí cómo
     * reacciona Hibernate si el trigger devuelve 0 filas afectadas.
     */
    private void vetarPersonaSistema(Persona persona) {
        if (ContextoSeguridad.CC_PERSONA_SISTEMA.equals(persona.getCc())) {
            throw new SecurityException("La persona del sistema no se puede modificar ni desactivar.");
        }
    }
}
