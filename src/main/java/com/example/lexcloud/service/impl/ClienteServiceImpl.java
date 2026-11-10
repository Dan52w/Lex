package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.ClienteMapper;
import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.ClienteService;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final Cliente_Repository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final ContextoSeguridad contextoSeguridad;

    @PersistenceContext
    private final EntityManager entityManager;

    // Constructor limpio: Solo dependemos de lo estrictamente necesario
    public ClienteServiceImpl(Cliente_Repository clienteRepository,
                              ClienteMapper clienteMapper,
                              EntityManager entityManager,
                              ContextoSeguridad contextoSeguridad) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.entityManager = entityManager;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    public ClienteResponseDTO guardar(ClienteRequestDTO request) {
        contextoSeguridad.verificarMismaFirma(request.idFirma());

        // 1. Validar que no exista un cliente activo con esa identificación en la misma firma
        clienteRepository.findByIdentificacionAndFirma(request.identificacion(), request.idFirma())
                .ifPresent(c -> {
                    throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.");
                });

        // 2. Mapear datos básicos directamente al nuevo objeto Cliente
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setIdentificacion(request.identificacion());
        nuevoCliente.setNombres(request.nombres());
        nuevoCliente.setPrimerApellido(request.primerApellido());
        nuevoCliente.setSegundoApellido(request.segundoApellido());
        nuevoCliente.setFechaNacimiento(request.fechaNacimiento());
        nuevoCliente.setLugarNacimiento(request.lugarNacimiento());
        nuevoCliente.setNacionalidad(request.nacionalidad());

        nuevoCliente.setCiudadResidencia(request.ciudadResidencia());
        nuevoCliente.setLocalidadSector(request.localidadSector());
        nuevoCliente.setBarrio(request.barrio());
        nuevoCliente.setDireccionResidencia(request.direccionResidencia());
        nuevoCliente.setTelefonoResidencia(request.telefonoResidencia());
        nuevoCliente.setDireccionOficina(request.direccionOficina());
        nuevoCliente.setTelefonoOficina(request.telefonoOficina());
        nuevoCliente.setTelefonoMovil(request.telefonoMovil());
        nuevoCliente.setCorreo(request.correo());

        nuevoCliente.setFechaVinculacion(LocalDate.now());
        nuevoCliente.setActivo(true);

        // 3. Asignar relaciones obligatorias con existencia validada (W4: el
        // getReference anterior fallaba tarde, en el flush, con un 500).
        nuevoCliente.setFirma(referenciaO404(Firma.class, request.idFirma(), "Firma"));
        nuevoCliente.setTipoCliente(referenciaO404(Tipo_Cliente.class, request.idTipoCliente(), "Tipo de cliente"));

        // 4. Asignar relaciones de caracterización opcionales
        nuevoCliente.setTipoIdentificacion(referenciaOpcional(Tipo_Identificacion.class, request.idTipoIdentificacion(), "Tipo de identificación"));
        nuevoCliente.setGenero(referenciaOpcional(Genero.class, request.idGenero(), "Género"));
        nuevoCliente.setEstadoCivil(referenciaOpcional(Estado_Civil.class, request.idEstadoCivil(), "Estado civil"));
        nuevoCliente.setEstrato(referenciaOpcional(Estrato_Economico.class, request.idEstrato(), "Estrato"));
        nuevoCliente.setVivienda(referenciaOpcional(Tipo_Vivienda.class, request.idVivienda(), "Tipo de vivienda"));
        nuevoCliente.setNivelSalarial(referenciaOpcional(Nivel_Salarial.class, request.idNivelSalarial(), "Nivel salarial"));
        nuevoCliente.setTipoEmpleado(referenciaOpcional(Tipo_Empleado.class, request.idTipoEmpleado(), "Tipo de empleado"));
        nuevoCliente.setGradoEscolaridad(referenciaOpcional(Grado_Escolaridad.class, request.idGradoEscolaridad(), "Grado de escolaridad"));
        nuevoCliente.setDiscapacidad(referenciaOpcional(Discapacidad.class, request.idDiscapacidad(), "Discapacidad"));

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
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarFirma(Integer idFirma) {
        return listarFirma(idFirma, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarFirma(Integer idFirma, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return clienteMapper.toResponseList(
                clienteRepository.findAllByFirmaId(idFirma, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro) {
        return buscarClientes(idFirma, filtro, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        // SVC-5: el filtro se escapa antes del LIKE (ver BusquedaUtil).
        return clienteMapper.toResponseList(
                clienteRepository.superBuscador(BusquedaUtil.escaparLike(filtro), idFirma, Paginacion.crear(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return clienteRepository.findByIdentificacionAndFirma(identificacion, idFirma)
                .map(clienteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return clienteRepository.findById(id)
                .filter(cliente -> cliente.getActivo() && cliente.getFirma().getId().equals(idFirma))
                .map(clienteMapper::toResponse);
    }

    @Override
    public ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));

        // Validación crítica de seguridad: ¿El cliente pertenece a la firma del abogado solicitante?
        if (!clienteExistente.getFirma().getId().equals(idFirma)) {
            throw new SecurityException("No tiene permisos para modificar un cliente perteneciente a otra firma.");
        }

        // Sin duplicados: si cambia la identificación, que no sea la de otro cliente de la firma.
        clienteRepository.findByIdentificacionAndFirma(request.identificacion(), idFirma)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.");
                });

        // Actualización de campos de texto
        clienteExistente.setIdentificacion(request.identificacion());
        clienteExistente.setNombres(request.nombres());
        clienteExistente.setPrimerApellido(request.primerApellido());
        clienteExistente.setSegundoApellido(request.segundoApellido());
        clienteExistente.setFechaNacimiento(request.fechaNacimiento());
        clienteExistente.setLugarNacimiento(request.lugarNacimiento());
        clienteExistente.setNacionalidad(request.nacionalidad());

        clienteExistente.setCiudadResidencia(request.ciudadResidencia());
        clienteExistente.setLocalidadSector(request.localidadSector());
        clienteExistente.setBarrio(request.barrio());
        clienteExistente.setDireccionResidencia(request.direccionResidencia());
        clienteExistente.setTelefonoResidencia(request.telefonoResidencia());
        clienteExistente.setDireccionOficina(request.direccionOficina());
        clienteExistente.setTelefonoOficina(request.telefonoOficina());
        clienteExistente.setTelefonoMovil(request.telefonoMovil());
        clienteExistente.setCorreo(request.correo());

        // Actualización de referencias (con existencia validada, ver guardar)
        clienteExistente.setTipoCliente(referenciaO404(Tipo_Cliente.class, request.idTipoCliente(), "Tipo de cliente"));

        clienteExistente.setTipoIdentificacion(referenciaOpcional(Tipo_Identificacion.class, request.idTipoIdentificacion(), "Tipo de identificación"));
        clienteExistente.setGenero(referenciaOpcional(Genero.class, request.idGenero(), "Género"));
        clienteExistente.setEstadoCivil(referenciaOpcional(Estado_Civil.class, request.idEstadoCivil(), "Estado civil"));
        clienteExistente.setEstrato(referenciaOpcional(Estrato_Economico.class, request.idEstrato(), "Estrato"));
        clienteExistente.setVivienda(referenciaOpcional(Tipo_Vivienda.class, request.idVivienda(), "Tipo de vivienda"));
        clienteExistente.setNivelSalarial(referenciaOpcional(Nivel_Salarial.class, request.idNivelSalarial(), "Nivel salarial"));
        clienteExistente.setTipoEmpleado(referenciaOpcional(Tipo_Empleado.class, request.idTipoEmpleado(), "Tipo de empleado"));
        clienteExistente.setGradoEscolaridad(referenciaOpcional(Grado_Escolaridad.class, request.idGradoEscolaridad(), "Grado de escolaridad"));
        clienteExistente.setDiscapacidad(referenciaOpcional(Discapacidad.class, request.idDiscapacidad(), "Discapacidad"));

        try {
            return clienteMapper.toResponse(clienteRepository.save(clienteExistente));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Ya existe un cliente con esta identificación en esta firma.", ex);
        }
    }

    @Override
    public void desactivarCliente(Integer id, Integer idFirma) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));

        // Validación crítica de seguridad para borrado lógico
        if (!cliente.getFirma().getId().equals(idFirma)) {
            throw new SecurityException("No tiene permisos para desactivar un cliente de otra firma.");
        }

        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    /**
     * Carga la entidad referenciada o 404 si no existe. A diferencia de
     * {@code getReference}, que devuelve un proxy sin tocar la BD y falla
     * tarde (en el flush, con 500), aquí el fallo es inmediato y claro.
     */
    private <T> T referenciaO404(Class<T> tipo, Integer id, String nombre) {
        if (id == null) {
            throw new IllegalArgumentException("Debe indicar " + nombre + ".");
        }
        T entidad = entityManager.find(tipo, id);
        if (entidad == null) {
            throw new EntityNotFoundException(nombre + " no encontrado con ID: " + id);
        }
        return entidad;
    }

    /** Referencia opcional: null si no viene id; 404 si el id no existe. */
    private <T> T referenciaOpcional(Class<T> tipo, Integer id, String nombre) {
        if (id == null) {
            return null;
        }
        return referenciaO404(tipo, id, nombre);
    }
}
