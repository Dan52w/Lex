package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.ClienteMapper;
import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.repositorios.Discapacidad_Repository;
import com.example.lexcloud.repositorios.EstadoCivil_Repository;
import com.example.lexcloud.repositorios.EstratoEconomico_Repository;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Genero_Repository;
import com.example.lexcloud.repositorios.GradoEscolaridad_Repository;
import com.example.lexcloud.repositorios.NivelSalarial_Repository;
import com.example.lexcloud.repositorios.TipoCliente_Repository;
import com.example.lexcloud.repositorios.TipoEmpleado_Repository;
import com.example.lexcloud.repositorios.TipoIdentificacion_Repository;
import com.example.lexcloud.repositorios.TipoVivienda_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.ClienteService;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    /** Zona horaria de negocio: las fechas (vinculación, nacimiento) se cortan aquí. */
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final Cliente_Repository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final ContextoSeguridad contextoSeguridad;
    // M24: las referencias se cargan por repositorio, no por EntityManager.
    private final Firma_Repository firmaRepository;
    private final TipoCliente_Repository tipoClienteRepository;
    private final TipoIdentificacion_Repository tipoIdentificacionRepository;
    private final Genero_Repository generoRepository;
    private final EstadoCivil_Repository estadoCivilRepository;
    private final EstratoEconomico_Repository estratoRepository;
    private final TipoVivienda_Repository viviendaRepository;
    private final NivelSalarial_Repository nivelSalarialRepository;
    private final TipoEmpleado_Repository tipoEmpleadoRepository;
    private final GradoEscolaridad_Repository gradoEscolaridadRepository;
    private final Discapacidad_Repository discapacidadRepository;

    // Constructor limpio: Solo dependemos de lo estrictamente necesario
    public ClienteServiceImpl(Cliente_Repository clienteRepository,
                              ClienteMapper clienteMapper,
                              ContextoSeguridad contextoSeguridad,
                              Firma_Repository firmaRepository,
                              TipoCliente_Repository tipoClienteRepository,
                              TipoIdentificacion_Repository tipoIdentificacionRepository,
                              Genero_Repository generoRepository,
                              EstadoCivil_Repository estadoCivilRepository,
                              EstratoEconomico_Repository estratoRepository,
                              TipoVivienda_Repository viviendaRepository,
                              NivelSalarial_Repository nivelSalarialRepository,
                              TipoEmpleado_Repository tipoEmpleadoRepository,
                              GradoEscolaridad_Repository gradoEscolaridadRepository,
                              Discapacidad_Repository discapacidadRepository) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.contextoSeguridad = contextoSeguridad;
        this.firmaRepository = firmaRepository;
        this.tipoClienteRepository = tipoClienteRepository;
        this.tipoIdentificacionRepository = tipoIdentificacionRepository;
        this.generoRepository = generoRepository;
        this.estadoCivilRepository = estadoCivilRepository;
        this.estratoRepository = estratoRepository;
        this.viviendaRepository = viviendaRepository;
        this.nivelSalarialRepository = nivelSalarialRepository;
        this.tipoEmpleadoRepository = tipoEmpleadoRepository;
        this.gradoEscolaridadRepository = gradoEscolaridadRepository;
        this.discapacidadRepository = discapacidadRepository;
    }

    @Override
    @Transactional
    public ClienteResponseDTO guardar(ClienteRequestDTO request) {
        // P0-B2: idFirma opcional; si no viene, se deriva del token. Sin
        // firma resoluble la creación se rechaza con 400 claro.
        Integer idFirma = request.idFirma() != null ? request.idFirma()
                : contextoSeguridad.idFirmaAutenticada().orElse(null);
        if (idFirma == null) {
            throw new IllegalArgumentException("No tienes una firma asignada; selecciona una firma");
        }
        validarRequest(request);
        contextoSeguridad.verificarMismaFirma(idFirma);

        // 1. Validar que no exista un cliente activo con esa identificación en la misma firma
        clienteRepository.findByIdentificacionAndFirma(request.identificacion().trim(), idFirma)
                .ifPresent(c -> {
                    throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.");
                });

        // 2. Mapear datos básicos directamente al nuevo objeto Cliente
        Cliente nuevoCliente = new Cliente();
        aplicarCamposTexto(nuevoCliente, request);

        // B6: la fecha de vinculación se corta en la zona de negocio, no en la
        // zona por defecto de la JVM.
        nuevoCliente.setFechaVinculacion(LocalDate.now(ZONA_BOGOTA));
        nuevoCliente.setActivo(true);

        // 3. Asignar relaciones obligatorias con existencia validada (W4: el
        // getReference anterior fallaba tarde, en el flush, con un 500).
        nuevoCliente.setFirma(referenciaO404(firmaRepository, idFirma, "Firma"));
        nuevoCliente.setTipoCliente(referenciaO404(tipoClienteRepository, request.idTipoCliente(), "Tipo de cliente"));

        // 4. Asignar relaciones de caracterización opcionales
        asignarCaracterizacion(nuevoCliente, request);

        try {
            return clienteMapper.toResponse(clienteRepository.save(nuevoCliente));
        } catch (DataIntegrityViolationException ex) {
            // Defensa en profundidad: la comprobación de arriba solo ve clientes
            // activos, pero la UNIQUE de BD es (identificacion, id_firma) sin
            // filtro de activo; un duplicado inactivo o una carrera entre dos
            // peticiones cae aquí con el mismo mensaje claro (409).
            throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.", ex);
        }
    }

    @Override
    public List<ClienteResponseDTO> listarFirma(Integer idFirma) {
        return listarFirma(idFirma, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<ClienteResponseDTO> listarFirma(Integer idFirma, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return clienteMapper.toResponseList(
                clienteRepository.findAllByFirmaId(idFirma, Paginacion.crear(page, size)));
    }

    @Override
    public List<ClienteResponseDTO> listarMiFirma(int page, int size) {
        // P0-B2: clientes de la firma del usuario autenticado. El admin ve
        // TODOS los clientes activos (todas las firmas); un usuario sin
        // firma resoluble ve lista vacía (no error: el dashboard y la vista
        // de clientes la toleran como "sin clientes").
        if (contextoSeguridad.esAdmin()) {
            return listarTodosActivos(page, size);
        }
        return contextoSeguridad.idFirmaAutenticada()
                .map(firmaId -> listarFirma(firmaId, page, size))
                .orElse(List.of());
    }

    @Override
    public List<ClienteResponseDTO> listarTodosActivos(int page, int size) {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede listar los clientes de todas las firmas.");
        }
        return clienteMapper.toResponseList(
                clienteRepository.findAllActivos(Paginacion.crear(page, size)));
    }

    /**
     * Resuelve la firma efectiva para las operaciones con idFirma opcional
     * (P0-B2). Precedencia: parámetro de la URL, luego idFirma del cuerpo,
     * luego la firma derivada del token. El admin no pasa por el check de
     * firma: si no hay ninguna, devuelve null y el llamador usa la firma
     * del propio recurso. Un no-admin sin firma resoluble recibe 403.
     */
    private Integer resolverIdFirma(Integer idFirmaParametro, Integer idFirmaCuerpo) {
        Integer resuelta = idFirmaParametro != null ? idFirmaParametro : idFirmaCuerpo;
        if (resuelta != null) {
            return resuelta;
        }
        if (contextoSeguridad.esAdmin()) {
            return null;
        }
        return contextoSeguridad.idFirmaAutenticada()
                .orElseThrow(() -> new SecurityException("No tienes una firma asignada; selecciona una firma"));
    }

    @Override
    public List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro) {
        return buscarClientes(idFirma, filtro, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        // SVC-5: el filtro se escapa antes del LIKE (ver BusquedaUtil).
        return clienteMapper.toResponseList(
                clienteRepository.superBuscador(BusquedaUtil.escaparLike(filtro), idFirma, Paginacion.crear(page, size)));
    }

    @Override
    public Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma) {
        // P0-B2: idFirma opcional (se deriva del token). Admin sin idFirma:
        // búsqueda en cualquier firma; no-admin sin firma: 403.
        Integer firmaEfectiva = resolverIdFirma(idFirma, null);
        if (firmaEfectiva == null) {
            return clienteRepository.findByIdentificacionActivo(identificacion == null ? null : identificacion.trim())
                    .map(clienteMapper::toResponse);
        }
        if (!contextoSeguridad.esAdmin()) {
            contextoSeguridad.verificarMismaFirma(firmaEfectiva);
        }
        return clienteRepository.findByIdentificacionAndFirma(identificacion == null ? null : identificacion.trim(), firmaEfectiva)
                .map(clienteMapper::toResponse);
    }

    @Override
    public Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma) {
        // P0-B2: idFirma opcional (se deriva del token). Admin sin idFirma:
        // el cliente es de cualquier firma; no-admin sin firma: 403.
        Integer firmaEfectiva = resolverIdFirma(idFirma, null);
        if (firmaEfectiva == null) {
            return clienteRepository.findById(id)
                    .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
                    .map(clienteMapper::toResponse);
        }
        if (!contextoSeguridad.esAdmin()) {
            contextoSeguridad.verificarMismaFirma(firmaEfectiva);
        }
        return clienteRepository.findById(id)
                .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo())
                        && cliente.getFirma().getId().equals(firmaEfectiva))
                .map(clienteMapper::toResponse);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request) {
        Validaciones.requerirId(id, "id");
        validarRequest(request);
        // P0-B2: idFirma opcional (URL, cuerpo o token). Admin sin ninguna:
        // la firma efectiva es la del propio cliente.
        Integer firmaEfectiva = resolverIdFirma(idFirma, request.idFirma());
        // M4: el idFirma del DTO ya no se ignora en silencio. La firma de un
        // cliente es inmutable por actualización: si el DTO trae otra firma,
        // se rechaza (400) en vez de actualizar con la del parámetro.
        if (request.idFirma() != null && firmaEfectiva != null && !request.idFirma().equals(firmaEfectiva)) {
            throw new IllegalArgumentException("La firma del cliente no se puede cambiar.");
        }
        if (firmaEfectiva != null && !contextoSeguridad.esAdmin()) {
            contextoSeguridad.verificarMismaFirma(firmaEfectiva);
        }
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // Validación crítica de seguridad: ¿El cliente pertenece a la firma del abogado solicitante?
        Integer firmaCliente = clienteExistente.getFirma().getId();
        if (firmaEfectiva == null) {
            firmaEfectiva = firmaCliente;
        }
        if (!firmaCliente.equals(firmaEfectiva)) {
            throw new SecurityException("No tiene permisos para modificar un cliente perteneciente a otra firma.");
        }

        // M9: un cliente inactivo no se edita; primero se reactiva por la vía
        // que corresponda (no existe reactivarCliente: la reactivación queda
        // pendiente de decisión, ver informe del lote).
        if (Boolean.FALSE.equals(clienteExistente.getActivo())) {
            throw new IllegalStateException("No se puede actualizar un cliente inactivo.");
        }

        // Sin duplicados: si cambia la identificación, que no sea la de otro cliente de la firma.
        clienteRepository.findByIdentificacionAndFirma(request.identificacion().trim(), firmaEfectiva)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.");
                });

        // Actualización de campos de texto (con trim, ver M4).
        aplicarCamposTexto(clienteExistente, request);

        // Actualización de referencias (con existencia validada, ver guardar)
        clienteExistente.setTipoCliente(referenciaO404(tipoClienteRepository, request.idTipoCliente(), "Tipo de cliente"));
        asignarCaracterizacion(clienteExistente, request);

        try {
            return clienteMapper.toResponse(clienteRepository.save(clienteExistente));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.", ex);
        }
    }

    @Override
    @Transactional
    public void desactivarCliente(Integer id, Integer idFirma) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirId(idFirma, "idFirma");
        contextoSeguridad.verificarMismaFirma(idFirma);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // Validación crítica de seguridad para borrado lógico
        if (!cliente.getFirma().getId().equals(idFirma)) {
            throw new SecurityException("No tiene permisos para desactivar un cliente de otra firma.");
        }

        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void reactivarCliente(Integer id, Integer idFirma) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirId(idFirma, "idFirma");
        // B3: reactivar es solo admin (desactivar lo puede hacer el abogado
        // de la firma; devolver un cliente de baja es decisión de gobierno).
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("No tiene permisos para reactivar un cliente.");
        }
        contextoSeguridad.verificarMismaFirma(idFirma);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        if (!cliente.getFirma().getId().equals(idFirma)) {
            throw new SecurityException("No tiene permisos para reactivar un cliente de otra firma.");
        }
        cliente.setActivo(true);
        clienteRepository.save(cliente);
    }

    // =========================================================================
    // M7 + M4: guardas de null al inicio de las escrituras y validación de la
    // fecha de nacimiento (obligatoria y no futura, cortada en America/Bogota).
    // =========================================================================

    private void validarRequest(ClienteRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        Validaciones.requerirNoBlank(request.identificacion(), "identificacion");
        Validaciones.requerirNoBlank(request.nombres(), "nombres");
        Validaciones.requerirNoBlank(request.primerApellido(), "primerApellido");
        Validaciones.requerirNoBlank(request.telefonoMovil(), "telefonoMovil");
        Validaciones.requerirNoBlank(request.correo(), "correo");
        // P0-B2: idFirma ya NO se exige aquí: es opcional en el DTO y se
        // resuelve en guardar/actualizar (URL, cuerpo o token).
        Validaciones.requerirId(request.idTipoCliente(), "idTipoCliente");
        validarFechaNacimiento(request.fechaNacimiento());
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) {
        Validaciones.requerirNoNulo(fechaNacimiento, "fechaNacimiento");
        if (fechaNacimiento.isAfter(LocalDate.now(ZONA_BOGOTA))) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
    }

    /** M4: todos los campos de texto se guardan sin espacios en los extremos. */
    private void aplicarCamposTexto(Cliente cliente, ClienteRequestDTO request) {
        cliente.setIdentificacion(recortar(request.identificacion()));
        cliente.setNombres(recortar(request.nombres()));
        cliente.setPrimerApellido(recortar(request.primerApellido()));
        cliente.setSegundoApellido(recortar(request.segundoApellido()));
        cliente.setFechaNacimiento(request.fechaNacimiento());
        cliente.setLugarNacimiento(recortar(request.lugarNacimiento()));
        cliente.setNacionalidad(recortar(request.nacionalidad()));
        cliente.setCiudadResidencia(recortar(request.ciudadResidencia()));
        cliente.setLocalidadSector(recortar(request.localidadSector()));
        cliente.setBarrio(recortar(request.barrio()));
        cliente.setDireccionResidencia(recortar(request.direccionResidencia()));
        cliente.setTelefonoResidencia(recortar(request.telefonoResidencia()));
        cliente.setDireccionOficina(recortar(request.direccionOficina()));
        cliente.setTelefonoOficina(recortar(request.telefonoOficina()));
        cliente.setTelefonoMovil(recortar(request.telefonoMovil()));
        cliente.setCorreo(recortar(request.correo()));
    }

    private void asignarCaracterizacion(Cliente cliente, ClienteRequestDTO request) {
        cliente.setTipoIdentificacion(referenciaOpcional(tipoIdentificacionRepository, request.idTipoIdentificacion(), "Tipo de identificación"));
        cliente.setGenero(referenciaOpcional(generoRepository, request.idGenero(), "Género"));
        cliente.setEstadoCivil(referenciaOpcional(estadoCivilRepository, request.idEstadoCivil(), "Estado civil"));
        cliente.setEstrato(referenciaOpcional(estratoRepository, request.idEstrato(), "Estrato"));
        cliente.setVivienda(referenciaOpcional(viviendaRepository, request.idVivienda(), "Tipo de vivienda"));
        cliente.setNivelSalarial(referenciaOpcional(nivelSalarialRepository, request.idNivelSalarial(), "Nivel salarial"));
        cliente.setTipoEmpleado(referenciaOpcional(tipoEmpleadoRepository, request.idTipoEmpleado(), "Tipo de empleado"));
        cliente.setGradoEscolaridad(referenciaOpcional(gradoEscolaridadRepository, request.idGradoEscolaridad(), "Grado de escolaridad"));
        cliente.setDiscapacidad(referenciaOpcional(discapacidadRepository, request.idDiscapacidad(), "Discapacidad"));
    }

    private static String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }

    /**
     * Carga la entidad referenciada o 404 si no existe. A diferencia de
     * {@code getReference}, que devuelve un proxy sin tocar la BD y falla
     * tarde (en el flush, con 500), aquí el fallo es inmediato y claro.
     * M24: se resuelve por repositorio, sin EntityManager.
     */
    private <T> T referenciaO404(JpaRepository<T, Integer> repositorio, Integer id, String nombre) {
        if (id == null) {
            throw new IllegalArgumentException("Debe indicar " + nombre + ".");
        }
        return repositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(nombre + " no encontrado"));
    }

    /** Referencia opcional: null si no viene id; 404 si el id no existe. */
    private <T> T referenciaOpcional(JpaRepository<T, Integer> repositorio, Integer id, String nombre) {
        if (id == null) {
            return null;
        }
        return referenciaO404(repositorio, id, nombre);
    }
}
