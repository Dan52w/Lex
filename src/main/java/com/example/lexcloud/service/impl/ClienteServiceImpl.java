package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.ClienteMapper;
import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.service.ClienteService;
import com.example.lexcloud.util.SearchTerm;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;

    @PersistenceContext
    private final EntityManager entityManager;

    // Constructor limpio: Solo dependemos de lo estrictamente necesario
    public ClienteServiceImpl(Cliente_Repository clienteRepository,
                              ClienteMapper clienteMapper,
                              EntityManager entityManager,
                              CurrentUser currentUser,
                              AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.entityManager = entityManager;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public ClienteResponseDTO guardar(ClienteRequestDTO request) {
        // P0-B2: la firma sale del token (admin puede indicar otra firma).
        Integer firmaId = currentUser.isAdmin() && request.idFirma() != null
                ? request.idFirma() : currentUser.requireFirmaId();
        if (firmaId == null) {
            throw new IllegalArgumentException("Debe indicar la firma del cliente");
        }
        // 1. Validar que no exista un cliente activo con esa identificación en la misma firma
        clienteRepository.findByIdentificacionAndFirma(request.identificacion(), firmaId)
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

        // 3. Asignar relaciones obligatorias utilizando proxies ligeros de JPA
        nuevoCliente.setFirma(entityManager.getReference(Firma.class, firmaId));
        nuevoCliente.setTipoCliente(entityManager.getReference(Tipo_Cliente.class, request.idTipoCliente()));

        // 4. Asignar relaciones de caracterización opcionales (evitamos SELECTs innecesarios en cascada)
        if (request.idTipoIdentificacion() != null) {
            nuevoCliente.setTipoIdentificacion(entityManager.getReference(Tipo_Identificacion.class, request.idTipoIdentificacion()));
        }
        if (request.idGenero() != null) {
            nuevoCliente.setGenero(entityManager.getReference(Genero.class, request.idGenero()));
        }
        if (request.idEstadoCivil() != null) {
            nuevoCliente.setEstadoCivil(entityManager.getReference(Estado_Civil.class, request.idEstadoCivil()));
        }
        if (request.idEstrato() != null) {
            nuevoCliente.setEstrato(entityManager.getReference(Estrato_Economico.class, request.idEstrato()));
        }
        if (request.idVivienda() != null) {
            nuevoCliente.setVivienda(entityManager.getReference(Tipo_Vivienda.class, request.idVivienda()));
        }
        if (request.idNivelSalarial() != null) {
            nuevoCliente.setNivelSalarial(entityManager.getReference(Nivel_Salarial.class, request.idNivelSalarial()));
        }
        if (request.idTipoEmpleado() != null) {
            nuevoCliente.setTipoEmpleado(entityManager.getReference(Tipo_Empleado.class, request.idTipoEmpleado()));
        }
        if (request.idGradoEscolaridad() != null) {
            nuevoCliente.setGradoEscolaridad(entityManager.getReference(Grado_Escolaridad.class, request.idGradoEscolaridad()));
        }
        if (request.idDiscapacidad() != null) {
            nuevoCliente.setDiscapacidad(entityManager.getReference(Discapacidad.class, request.idDiscapacidad()));
        }

        ClienteResponseDTO creado = clienteMapper.toResponse(clienteRepository.save(nuevoCliente));
        // P1-A1: creación de cliente auditada (autor = token).
        auditoriaService.registrar(currentUser.personaId(), firmaId, "CLIENTE_CREADO", "cliente", creado.id(), null);
        return creado;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClienteResponseDTO> listarFirma(Integer idFirma, org.springframework.data.domain.Pageable pageable) {
        Integer firmaToken = currentUser.isAdmin() && idFirma != null
                ? idFirma : currentUser.requireFirmaId();
        return clienteRepository.findAllByFirmaId(firmaToken, pageable).map(clienteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro, org.springframework.data.domain.Pageable pageable) {
        Integer firmaToken = currentUser.isAdmin() && idFirma != null
                ? idFirma : currentUser.requireFirmaId();
        return clienteRepository.superBuscador(SearchTerm.escape(filtro), firmaToken, pageable).map(clienteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarFirma(Integer idFirma) {
        // P0-B2: idFirma de la URL se ignora; siempre la firma del token.
        Integer firmaToken = currentUser.isAdmin() && idFirma != null
                ? idFirma : currentUser.requireFirmaId();
        return clienteMapper.toResponseList(clienteRepository.findAllByFirmaId(firmaToken));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro) {
        // P0-B2: idFirma del cliente se ignora.
        Integer firmaToken = currentUser.isAdmin() && idFirma != null
                ? idFirma : currentUser.requireFirmaId();
        return clienteMapper.toResponseList(clienteRepository.superBuscador(SearchTerm.escape(filtro), firmaToken));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma) {
        Integer firmaToken = currentUser.isAdmin() && idFirma != null
                ? idFirma : currentUser.requireFirmaId();
        return clienteRepository.findByIdentificacionAndFirma(identificacion, firmaToken)
                .map(clienteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma) {
        // P0-B2: la firma contra la que se compara es la del token.
        return clienteRepository.findById(id)
                .filter(cliente -> cliente.getActivo() && currentUser.canAccessFirma(cliente.getFirma().getId()))
                .map(clienteMapper::toResponse);
    }

    @Override
    public ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));

        // P0-B2: la firma contra la que se compara es la del token.
        if (!currentUser.canAccessFirma(clienteExistente.getFirma().getId())) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }

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

        // Actualización de referencias
        clienteExistente.setTipoCliente(entityManager.getReference(Tipo_Cliente.class, request.idTipoCliente()));

        clienteExistente.setTipoIdentificacion(request.idTipoIdentificacion() != null ?
                entityManager.getReference(Tipo_Identificacion.class, request.idTipoIdentificacion()) : null);
        clienteExistente.setGenero(request.idGenero() != null ?
                entityManager.getReference(Genero.class, request.idGenero()) : null);
        clienteExistente.setEstadoCivil(request.idEstadoCivil() != null ?
                entityManager.getReference(Estado_Civil.class, request.idEstadoCivil()) : null);
        clienteExistente.setEstrato(request.idEstrato() != null ?
                entityManager.getReference(Estrato_Economico.class, request.idEstrato()) : null);
        clienteExistente.setVivienda(request.idVivienda() != null ?
                entityManager.getReference(Tipo_Vivienda.class, request.idVivienda()) : null);
        clienteExistente.setNivelSalarial(request.idNivelSalarial() != null ?
                entityManager.getReference(Nivel_Salarial.class, request.idNivelSalarial()) : null);
        clienteExistente.setTipoEmpleado(request.idTipoEmpleado() != null ?
                entityManager.getReference(Tipo_Empleado.class, request.idTipoEmpleado()) : null);
        clienteExistente.setGradoEscolaridad(request.idGradoEscolaridad() != null ?
                entityManager.getReference(Grado_Escolaridad.class, request.idGradoEscolaridad()) : null);
        clienteExistente.setDiscapacidad(request.idDiscapacidad() != null ?
                entityManager.getReference(Discapacidad.class, request.idDiscapacidad()) : null);

        ClienteResponseDTO actualizado = clienteMapper.toResponse(clienteRepository.save(clienteExistente));
        // P1-A1: edición de cliente auditada.
        auditoriaService.registrar(currentUser.personaId(), clienteExistente.getFirma().getId(), "CLIENTE_EDITADO", "cliente", id, null);
        return actualizado;
    }

    @Override
    public void desactivarCliente(Integer id, Integer idFirma) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));

        // P0-B2: la firma contra la que se compara es la del token.
        if (!currentUser.canAccessFirma(cliente.getFirma().getId())) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }

        cliente.setActivo(false);
        clienteRepository.save(cliente);
        // P1-A1: borrado lógico de cliente auditado.
        auditoriaService.registrar(currentUser.personaId(), cliente.getFirma().getId(), "CLIENTE_DESACTIVADO", "cliente", id, null);
    }
}