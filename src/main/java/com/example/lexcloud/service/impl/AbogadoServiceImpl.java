package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.AbogadoMapper;
import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Tipo_Abogado;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.TipoAbogado_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbogadoService;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AbogadoServiceImpl implements AbogadoService {

    private final Abogado_Repository abogadoRepository;
    private final Persona_Repository personaRepository;
    private final Firma_Repository firmaRepository;
    private final TipoAbogado_Repository tipoAbogadoRepository;
    private final AbogadoMapper abogadoMapper;
    private final ContextoSeguridad contextoSeguridad;

    // Constructor para inyección (más limpio que @Autowired en campos)
    public AbogadoServiceImpl(Abogado_Repository abogadoRepository,
                              Persona_Repository personaRepository,
                              Firma_Repository firmaRepository,
                              TipoAbogado_Repository tipoAbogadoRepository,
                              AbogadoMapper abogadoMapper,
                              ContextoSeguridad contextoSeguridad) {
        this.abogadoRepository = abogadoRepository;
        this.personaRepository = personaRepository;
        this.firmaRepository = firmaRepository;
        this.tipoAbogadoRepository = tipoAbogadoRepository;
        this.abogadoMapper = abogadoMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public AbogadoResponseDTO guardar(AbogadoRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        // N5: crear abogados es solo admin (antes cualquier miembro podía
        // crear un registro de abogado para cualquier persona).
        exigirAdmin();
        Validaciones.requerirId(request.idPersona(), "idPersona");
        // P0-B2 + N11: idFirma opcional, resuelta por el método central
        // (explícita > token). Sin firma resoluble la creación se rechaza
        // con 400 claro.
        Integer idFirma = contextoSeguridad.resolverIdFirmaEfectiva(request.idFirma());
        if (idFirma == null) {
            throw new IllegalArgumentException("No tienes una firma asignada; selecciona una firma");
        }
        Validaciones.requerirId(request.idTipoAbogado(), "idTipoAbogado");
        String tarjeta = Validaciones.requerirNoBlank(request.tarjeta_Profesional(), "tarjeta_Profesional").trim();
        contextoSeguridad.verificarMismaFirma(idFirma);

        // 1. Convertir DTO a Entidad base
        Abogado abogado = abogadoMapper.toEntity(request);
        abogado.setTarjeta_Profesional(tarjeta);

        // 2. Cargar relaciones físicas desde la BD
        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        if (Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("No se puede crear un abogado para una persona inactiva.");
        }
        if (ContextoSeguridad.CC_PERSONA_SISTEMA.equals(persona.getCc())) {
            throw new IllegalArgumentException("La persona del sistema no puede tener registro de abogado.");
        }
        abogado.setPersona(persona);
        // Sin duplicados: una persona solo puede tener un registro de abogado
        // (la BD tiene UNIQUE en id_persona).
        if (abogadoRepository.findByPersonaId(request.idPersona()).isPresent()) {
            throw new IllegalStateException("La persona ya tiene un registro de abogado.");
        }
        // ... setear firma y tipo abogado ...
        abogado.setFirma(firmaRepository.findById(idFirma)
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada")));
        // N6: la firma del sistema no admite abogados (es la firma
        // contenedor del caso raíz, no una firma operativa).
        if (contextoSeguridad.esFirmaSistema(abogado.getFirma())) {
            throw new IllegalArgumentException("La firma del sistema no admite abogados.");
        }
        abogado.setTipo_Abogado(tipoAbogadoRepository.findById(request.idTipoAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Especialización no encontrada")));
        abogado.setActivo(true);

        // 3. Guardar y retornar el ResponseDTO
        // A3: el catch distingue el duplicado real (carrera entre dos
        // peticiones) de cualquier otra violación de integridad, que se
        // re-lanza encadenando la causa en vez de tragársela.
        try {
            return abogadoMapper.toResponse(abogadoRepository.save(abogado));
        } catch (DataIntegrityViolationException ex) {
            if (abogadoRepository.findByPersonaId(request.idPersona()).isPresent()) {
                throw new IllegalStateException("La persona ya tiene un registro de abogado.");
            }
            throw new IllegalStateException("No se pudo guardar el abogado. Verifique los datos e intente nuevamente.", ex);
        }
    }

    @Override
    public List<AbogadoResponseDTO> listarTodos(Integer firmaId) {
        return listarTodos(firmaId, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AbogadoResponseDTO> listarTodos(Integer firmaId, int page, int size) {
        contextoSeguridad.verificarMismaFirma(firmaId);
        // Usamos el repositorio que creamos antes para filtrar por firma y activos
        List<Abogado> abogados = abogadoRepository.findByAbogadosByFirmaActivos(firmaId, Paginacion.crear(page, size));

        // El mapper se encarga de convertir toda la lista de un golpe
        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarTodosActivos(int page, int size) {
        // P0-B2 + N2: listado base para la vista /equipo del admin. Solo
        // admin: CON firma ve solo su firma; SIN firma (bootstrap) ve todas.
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede listar los abogados de todas las firmas.");
        }
        Optional<Integer> firmaPropia = contextoSeguridad.idFirmaAutenticada();
        if (firmaPropia.isPresent()) {
            return abogadoMapper.toResponseList(
                    abogadoRepository.findByAbogadosByFirmaActivos(firmaPropia.get(), Paginacion.crear(page, size)));
        }
        return abogadoMapper.toResponseList(
                abogadoRepository.findAllActivos(Paginacion.crear(page, size)));
    }

    @Override
    public List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad) {
        return listarTipo(idFirma, especialidad, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        List<Abogado> abogados = abogadoRepository.findByFirmaIdAndTipoAbogadoNombreIgnoreCase(
                idFirma, especialidad, Paginacion.crear(page, size));

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre) {
        return listarNombre(idFirma, nombre, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        // SVC-5: el nombre se escapa antes del LIKE (ver BusquedaUtil).
        List<Abogado> abogados = abogadoRepository.findByNombre(
                idFirma, BusquedaUtil.escaparLike(nombre), Paginacion.crear(page, size));

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorId(Integer id) {
        return abogadoRepository.findById(id)
                .filter(this::perteneceAFirmaAutenticada)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorCc(String cc, Integer idFirma) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return  abogadoRepository.findByAbogadoActivoByCc(cc, idFirma)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorPersonaId(Integer id) {
        return  abogadoRepository.findByPersonaId(id)
                .filter(this::perteneceAFirmaAutenticada)
                .map(abogadoMapper::toResponse);
    }

    @Override
    @Transactional
    public AbogadoResponseDTO actualizar(Integer id, AbogadoRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        // N5: actualizar abogados es solo admin.
        exigirAdmin();
        Validaciones.requerirId(request.idPersona(), "idPersona");
        Validaciones.requerirId(request.idFirma(), "idFirma");
        Validaciones.requerirId(request.idTipoAbogado(), "idTipoAbogado");
        String tarjeta = Validaciones.requerirNoBlank(request.tarjeta_Profesional(), "tarjeta_Profesional").trim();

        Abogado oldAbogado = abogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));
        // Solo se puede modificar un abogado de la propia firma, y no se
        // puede mover a otra firma.
        contextoSeguridad.verificarAccesoAAbogado(oldAbogado);
        contextoSeguridad.verificarMismaFirma(request.idFirma());

        // M9 (mismo criterio que Cliente): un abogado inactivo no se edita;
        // primero se reactiva con reactivarAbogado.
        if (Boolean.FALSE.equals(oldAbogado.getActivo())) {
            throw new IllegalStateException("No se puede actualizar un abogado inactivo.");
        }

        // M5: validar la persona destino — debe existir, estar activa, no ser
        // la persona del sistema y no tener ya otro registro de abogado.
        Persona personaDestino = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        if (Boolean.FALSE.equals(personaDestino.getActivo())) {
            throw new IllegalArgumentException("No se puede asignar un abogado a una persona inactiva.");
        }
        if (ContextoSeguridad.CC_PERSONA_SISTEMA.equals(personaDestino.getCc())) {
            throw new IllegalArgumentException("La persona del sistema no puede tener registro de abogado.");
        }
        abogadoRepository.findByPersonaId(request.idPersona())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalStateException("La persona ya tiene un registro de abogado.");
                });

        // 1. Buscamos las nuevas entidades relacionadas por su ID
        Firma nuevaFirma = firmaRepository.findById(request.idFirma())
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada"));
        Tipo_Abogado nuevoTipo = tipoAbogadoRepository.findById(request.idTipoAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Especialización no encontrada"));

        // 2. Actualizamos los campos de la entidad persistida
        oldAbogado.setPersona(personaDestino);
        oldAbogado.setFirma(nuevaFirma);
        oldAbogado.setTipo_Abogado(nuevoTipo);
        oldAbogado.setTarjeta_Profesional(tarjeta);

        // 3. Guardamos y mapeamos a respuesta (A3: mismo catch que en guardar).
        try {
            return abogadoMapper.toResponse(abogadoRepository.save(oldAbogado));
        } catch (DataIntegrityViolationException ex) {
            if (abogadoRepository.findByPersonaId(request.idPersona())
                    .filter(otro -> !otro.getId().equals(id)).isPresent()) {
                throw new IllegalStateException("La persona ya tiene un registro de abogado.");
            }
            throw new IllegalStateException("No se pudo actualizar el abogado. Verifique los datos e intente nuevamente.", ex);
        }
    }

    @Override
    @Transactional
    public void desactivarAbogado(Integer idAbogado) {
        Validaciones.requerirId(idAbogado, "idAbogado");
        // N5: desactivar abogados es solo admin.
        exigirAdmin();
        // A5: findById sin filtro de activo — un abogado ya inactivo debe
        // encontrarse igual (la operación es idempotente); las consultas de
        // listado filtran por activo y no servirían para localizarlo.
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));

        contextoSeguridad.verificarAccesoAAbogado(abogado);

        // A5: la baja es SOLO del registro de abogado (flag propio
        // Abogado.activo). La Persona NO se toca: desactivar al abogado no
        // cierra su login, no le quita el rol ni lo saca de los listados de
        // personas. Si la persona debe darse de baja, eso se hace en
        // PersonaService.desactivar.
        abogado.setActivo(false);
        abogadoRepository.save(abogado);
    }

    @Override
    @Transactional
    public void reactivarAbogado(Integer id) {
        Validaciones.requerirId(id, "id");
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede reactivar un abogado.");
        }
        // findById sin filtro de activo: el registro a reactivar es, por
        // definición, inactivo y las consultas de listado no lo encontrarían.
        Abogado abogado = abogadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));
        contextoSeguridad.verificarAccesoAAbogado(abogado);
        // Solo se reactiva el flag del abogado; si su persona sigue inactiva,
        // el abogado continúa fuera de los listados (que exigen persona
        // activa) hasta que la persona se reactive en PersonaService.
        abogado.setActivo(true);
        abogadoRepository.save(abogado);
    }

    /**
     * N5: crear, actualizar y desactivar abogados es solo admin (mismo
     * patrón que FirmaServiceImpl.exigirAdmin). Reactivar ya lo exigía.
     */
    private void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede realizar esta operación.");
        }
    }

    /**
     * S2: un abogado de otra firma no existe para quien consulta (se devuelve
     * vacío, como hace ClienteService.buscarPorId). C4 (fail-closed): si la
     * firma del usuario no se puede resolver (sin registro de abogado), la
     * consulta se deniega; nunca se permite por no poder verificar.
     */
    private boolean perteneceAFirmaAutenticada(Abogado abogado) {
        return contextoSeguridad.idFirmaAutenticada()
                .map(firmaId -> abogado.getFirma() != null
                        && firmaId.equals(abogado.getFirma().getId()))
                .orElse(false);
    }
}
