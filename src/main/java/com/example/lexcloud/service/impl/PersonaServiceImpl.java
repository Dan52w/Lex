package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.PersonaMapper;
import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Rol;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AuditoriaSeguridadService;
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
    private final Firma_Repository firmaRepository;
    private final PersonaMapper personaMapper;
    private final ContextoSeguridad contextoSeguridad;
    private final AuditoriaSeguridadService auditoriaService;

    public PersonaServiceImpl(Persona_Repository personaRepository, Rol_Repository rolRepository,
                              Firma_Repository firmaRepository,
                              PersonaMapper personaMapper, ContextoSeguridad contextoSeguridad,
                              AuditoriaSeguridadService auditoriaService) {
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.firmaRepository = firmaRepository;
        this.personaMapper = personaMapper;
        this.contextoSeguridad = contextoSeguridad;
        this.auditoriaService = auditoriaService;
    }

    /** C3: las escrituras de personas son solo para admin (salvo la excepción A12). */
    private void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede modificar personas.");
        }
    }

    /**
     * V5: resuelve la firma que se asigna a una persona nueva (o al
     * actualizarla): el súper admin fija la que venga (o null, sin firma);
     * el admin de firma siempre la suya, venga o no en el cuerpo — un
     * idFirma distinto se rechaza, nunca se aplica en silencio. La firma
     * del sistema está vetada para todos.
     */
    private Integer resolverIdFirmaParaPersona(Integer idFirmaSolicitada) {
        if (contextoSeguridad.esSuperAdmin()) {
            if (idFirmaSolicitada == null) {
                return null;
            }
            validarFirmaOperativa(idFirmaSolicitada);
            return idFirmaSolicitada;
        }
        Optional<Integer> propia = contextoSeguridad.idFirmaAutenticada();
        if (propia.isEmpty()) {
            // Fail-closed: un admin sin firma resoluble no puede asignar
            // personas a ninguna firma (antes de la V5 este caso no existía
            // para personas: no había id_firma que fijar).
            throw new SecurityException("El administrador no tiene una firma asociada.");
        }
        if (idFirmaSolicitada != null && !idFirmaSolicitada.equals(propia.get())) {
            throw new SecurityException("No tiene permisos para asignar personas a otra firma.");
        }
        return propia.get();
    }

    /** La firma existe y no es la del sistema. */
    private void validarFirmaOperativa(Integer idFirma) {
        var firma = firmaRepository.findById(idFirma)
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada"));
        if (contextoSeguridad.esFirmaSistema(firma)) {
            throw new IllegalArgumentException("La firma del sistema no admite personas.");
        }
    }

    /**
     * V5: un admin de firma solo gestiona personas de su firma. El súper
     * admin gestiona cualquiera; la propia persona queda fuera de este
     * chequeo (verificarMismaPersona ya la autoriza: editarse a uno mismo
     * no es gestionar otra firma). Fail-closed si alguna firma no se puede
     * determinar.
     */
    private void verificarAlcancePersona(Integer idPersonaObjetivo) {
        if (idPersonaObjetivo == null) {
            throw new IllegalArgumentException("Debe indicar la persona.");
        }
        if (idPersonaObjetivo.equals(contextoSeguridad.idPersonaAutenticada())) {
            return;
        }
        if (contextoSeguridad.esSuperAdmin()) {
            return;
        }
        Optional<Integer> miFirma = contextoSeguridad.idFirmaAutenticada();
        Optional<Integer> firmaObjetivo =
                contextoSeguridad.resolverIdFirmaDePersona(idPersonaObjetivo);
        if (miFirma.isEmpty() || firmaObjetivo.isEmpty()
                || !miFirma.get().equals(firmaObjetivo.get())) {
            throw new SecurityException("No tiene permisos para gestionar personas de otra firma.");
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

        // V5: crear personas con rol admin (ADMINISTRADOR o SUPER_ADMIN) es
        // SOLO súper admin. El admin de firma crea miembros con roles
        // no-admin de su propia firma. El rol se carga aquí (antes se
        // cargaba al final) porque la guarda lo necesita.
        Rol rol = rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado"));
        if (ContextoSeguridad.esRolAdmin(rol) && !contextoSeguridad.esSuperAdmin()) {
            throw new SecurityException("Solo un súper administrador puede crear personas con rol administrador.");
        }

        // 1. Validar unicidad de la CC
        if (personaRepository.findByCc(cc).isPresent()) {
            throw new IllegalStateException("Ya existe una persona registrada con esa cédula.");
        }

        PersonaRequestDTO normalizado = new PersonaRequestDTO(
                request.nombre().trim(), request.apellido().trim(), cc,
                request.idRol(), request.telefono().trim(), request.fechaNacimiento(),
                request.idFirma());
        Persona persona = personaMapper.toEntity(normalizado);

        persona.setRol(rol);

        // V5: fija personas.id_firma (el mapper no la copia: la resuelve el
        // servicio tras validar el alcance). Admin de firma -> su firma;
        // súper admin -> la solicitada o null.
        Integer idFirma = resolverIdFirmaParaPersona(request.idFirma());
        persona.setIdFirma(idFirma);

        // Aseguramos que por defecto esté activo al crear
        persona.setActivo(true);

        Persona guardada;
        try {
            guardada = personaRepository.save(persona);
        } catch (DataIntegrityViolationException ex) {
            // Defensa en profundidad (carrera entre dos peticiones con la misma
            // CC): re-consultar para distinguir el duplicado real de otra
            // violación de integridad, que se re-lanza con la causa.
            if (personaRepository.findByCc(cc).isPresent()) {
                throw new IllegalStateException("Ya existe una persona registrada con esa cédula.");
            }
            throw new IllegalStateException("No se pudo guardar la persona. Verifique los datos e intente nuevamente.", ex);
        }
        // V5: crear admin queda en auditoria. Fuera del try de arriba: un
        // fallo de auditoría no debe traducirse como "CC duplicada".
        if (ContextoSeguridad.esRolAdmin(rol)) {
            auditoriaService.registrar(AuditoriaSeguridadService.ACCION_CREAR_ADMIN,
                    "PERSONA", guardada.getId(), guardada.getIdFirma(),
                    "Admin creado: " + guardada.getNombre() + " " + guardada.getApellido()
                            + " (cc " + guardada.getCc() + ", rol " + rol.getNombre() + ")");
        }
        return personaMapper.toResponse(guardada);
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
        // V5: el admin de firma solo lista las personas de su firma (por
        // firma efectiva: id_firma directo o vía abogado). El súper admin
        // lista todas. Un admin sin firma resoluble (bootstrap, legado) no
        // lista ninguna: fail-closed, nunca global por no poder verificar.
        if (contextoSeguridad.esSuperAdmin()) {
            return personaMapper.toResponseList(
                    personaRepository.findAll(Paginacion.crear(page, size)).getContent());
        }
        return contextoSeguridad.idFirmaAutenticada()
                .map(firma -> personaMapper.toResponseList(
                        personaRepository.findByFirmaEfectiva(firma, Paginacion.crear(page, size))))
                .orElse(List.of());
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
        // V5: un admin de firma solo edita personas de su firma (la propia
        // persona queda fuera del chequeo, ver verificarAlcancePersona).
        verificarAlcancePersona(id);

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
        // V5: tocar un rol admin (ponerlo o quitarlo) es SOLO súper admin:
        // un admin de firma no puede crear otro admin ni degradar uno.
        boolean cambiaRol = (oldPersona.getRol() == null && request.idRol() != null)
                || (oldPersona.getRol() != null && !oldPersona.getRol().getId().equals(request.idRol()));
        Rol nuevoRol = null;
        if (cambiaRol) {
            if (!contextoSeguridad.esAdmin()) {
                throw new SecurityException("Solo un administrador puede cambiar el rol de una persona.");
            }
            nuevoRol = rolRepository.findById(request.idRol())
                    .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado"));
            if ((ContextoSeguridad.esRolAdmin(nuevoRol) || ContextoSeguridad.esRolAdmin(oldPersona.getRol()))
                    && !contextoSeguridad.esSuperAdmin()) {
                throw new SecurityException("Solo un súper administrador puede asignar o quitar un rol administrador.");
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

        // V5: la firma solo cambia si viene en el cuerpo (null = se deja
        // como está, nunca se borra en silencio). El alcance se valida en
        // resolverIdFirmaParaPersona (admin de firma -> solo la suya). Un
        // no-admin no puede cambiar la firma de nadie, ni la suya.
        boolean eraAdmin = ContextoSeguridad.esRolAdmin(oldPersona.getRol());
        if (request.idFirma() != null) {
            if (!contextoSeguridad.esAdmin()) {
                throw new SecurityException("Solo un administrador puede cambiar la firma de una persona.");
            }
            oldPersona.setIdFirma(resolverIdFirmaParaPersona(request.idFirma()));
        }

        oldPersona.setRol(nuevoRol != null ? nuevoRol : oldPersona.getRol());

        Persona actualizada = personaRepository.save(oldPersona);
        // V5: cambiar un rol a admin queda en auditoria (solo cuando el rol
        // resultante es admin y el anterior no lo era).
        if (cambiaRol && !eraAdmin && ContextoSeguridad.esRolAdmin(actualizada.getRol())) {
            auditoriaService.registrar(AuditoriaSeguridadService.ACCION_CAMBIO_ROL_A_ADMIN,
                    "PERSONA", actualizada.getId(), actualizada.getIdFirma(),
                    "Rol cambiado a admin: " + actualizada.getNombre() + " " + actualizada.getApellido()
                            + " (cc " + actualizada.getCc() + ", rol " + actualizada.getRol().getNombre() + ")");
        }
        return personaMapper.toResponse(actualizada);
    }

    @Override
    public boolean existeCc(String cc) {
        // A12: oráculo de existencia por cédula. Un admin ve el valor real;
        // un no-admin solo obtiene true para su propia cédula; cualquier otra
        // cédula (exista o no) responde false, sin filtrar nada.
        // V5: el admin de firma solo ve el valor real dentro de su firma:
        // una cédula de otra firma responde false, sin filtrar nada.
        if (cc == null) {
            return false;
        }
        if (contextoSeguridad.esSuperAdmin()) {
            return personaRepository.existsByCc(cc);
        }
        if (contextoSeguridad.esAdmin()) {
            return personaRepository.findByCc(cc)
                    .map(persona -> contextoSeguridad.resolverIdFirmaDePersona(persona.getId())
                            .map(firma -> firma.equals(contextoSeguridad.idFirmaAutenticada().orElse(null)))
                            .orElse(false))
                    .orElse(false);
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
        // V5: un admin de firma solo desactiva personas de su firma.
        verificarAlcancePersona(id);
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
        // V5: un admin de firma solo reactiva personas de su firma.
        verificarAlcancePersona(id);
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        vetarPersonaSistema(persona);
        persona.setActivo(true);
        personaRepository.save(persona);
    }

    /**
     * A12 (lecturas): admin o la propia persona; el resto ve vacío, sin oráculo.
     * V5: el admin de firma solo ve personas de su firma; el súper admin ve
     * cualquiera. Sin firma resoluble en alguno de los dos lados, no se ve
     * (fail-closed, indistinguible de "no existe").
     */
    private boolean visibleParaLlamador(Persona persona) {
        if (persona.getId().equals(contextoSeguridad.idPersonaAutenticada())) {
            return true;
        }
        if (contextoSeguridad.esSuperAdmin()) {
            return true;
        }
        if (!contextoSeguridad.esAdmin()) {
            return false;
        }
        return contextoSeguridad.resolverIdFirmaDePersona(persona.getId())
                .map(firma -> firma.equals(contextoSeguridad.idFirmaAutenticada().orElse(null)))
                .orElse(false);
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
