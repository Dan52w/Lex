package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.CasoMapper;
import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.service.CasoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CasoServiceImpl implements CasoService {

    private final Caso_Repository casoRepository;
    private final Estado_Repository estadoRepository;
    private final SubtipoCaso_Repository subtipoRepository;
    private final Persona_Repository personaRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Vinculo_Repository vinculoRepository;
    private final HistorialCambio_Repository historialCambioRepository;
    private final CasoMapper casoMapper;

    public CasoServiceImpl(Caso_Repository casoRepository,
                           Estado_Repository estadoRepository,
                           SubtipoCaso_Repository subtipoRepository,
                           Persona_Repository personaRepository,
                           VinculoCaso_Repository vinculoCasoRepository,
                           HistorialCambio_Repository historialCambioRepository,
                           Vinculo_Repository vinculoRepository,
                           CasoMapper casoMapper) {
        this.casoRepository = casoRepository;
        this.estadoRepository = estadoRepository;
        this.subtipoRepository = subtipoRepository;
        this.personaRepository = personaRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.historialCambioRepository = historialCambioRepository;
        this.vinculoRepository = vinculoRepository;
        this.casoMapper = casoMapper;
    }

    @Override
    public CasoResponseDTO guardar(CasoRequestDTO request) {
        Caso caso = casoMapper.toEntity(request);

        // 1. Relaciones obligatorias
        caso.setEstado(estadoRepository.findById(request.idEstado())
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado")));
        caso.setSubtipo_Caso(subtipoRepository.findById(request.idSubtipo())
                .orElseThrow(() -> new EntityNotFoundException("Subtipo no encontrado")));

        // 2. Manejo de Caso Padre (Por defecto ID 1 si viene null)
        Integer idPadreEfectivo = (request.idCasoPadre() != null) ? request.idCasoPadre() : 1;

        Caso padre = casoRepository.findById(idPadreEfectivo)
                .orElseThrow(() -> new EntityNotFoundException("Caso padre no encontrado (ID: " + idPadreEfectivo + ")"));

        caso.setCasoPadre(padre);

        Caso casoGuardado = casoRepository.save(caso);

        // 3. Vínculo automático para el Abogado/Persona que crea y para el Cliente
        crearVinculo(casoGuardado, request.idPersonaCreador(), request.idCliente(), true);

        return casoMapper.toResponse(casoGuardado);
    }

    private void crearVinculo(Caso caso, Integer idPersona, Integer idCliente, boolean autorizado) {
        // getReferenceById no hace SELECT, solo prepara la referencia para el INSERT
        Persona persona = personaRepository.getReferenceById(idPersona);
        Persona cliente = personaRepository.getReferenceById(idCliente);
        Vinculo tipoVinculo = vinculoRepository.getReferenceById(2);

        Vinculo_Caso vinculoCaso = new Vinculo_Caso();
        vinculoCaso.setCaso(caso);
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setVinculo(tipoVinculo);
        vinculoCaso.setAutorizado(autorizado);

        vinculoCasoRepository.save(vinculoCaso);
    }

    @Override
    public List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona) {
        // El repositorio ya filtra por c.activo = true y vc.autorizado = true
        // Solo añadimos un filtro extra por si el ID 1 se coló en los vínculos
        return casoRepository.findCasosAutorizadosPorPersona(idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public  List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona) {
        return casoRepository.buscarPorTituloParcial(titulo, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona) {
        return casoRepository.findByEstadoAndPersona(estadoNombre, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona) {
        return casoRepository.findBySubtipoIdAndPersona(idSubtipo, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo) {
        return casoRepository.findByEstadoAndSubtipoAndPersona(idPersona, idEstado, idSubtipo).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CasoResponseDTO buscarPorId(Integer id) {
        // REGLA DE ORO: El usuario no puede consultar el ID 1 directamente
        if (id == 1) {
            throw new EntityNotFoundException("Caso no encontrado");
        }
        return casoRepository.findById(id)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
    }

    @Override
    @Transactional
    public CasoResponseDTO actualizar(Integer id, CasoRequestDTO request) {
        // 1. Regla de oro: El caso raíz es intocable
        if (id == 1) {
            throw new EntityNotFoundException("No se puede modificar el caso raíz");
        }

        return casoRepository.findById(id).map(oldCaso -> {
            // 2. Capturamos el estado anterior para el historial
            String estadoAnterior = (oldCaso.getEstado() != null) ? oldCaso.getEstado().getNombre() : "Sin estado";

            // 3. Actualizamos datos básicos
            oldCaso.setTitulo(request.titulo());
            oldCaso.setDescripcion(request.descripcion());
            oldCaso.setEstado(estadoRepository.findById(request.idEstado())
                    .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado")));

            // 5. Lógica de Caso Padre (Si es null, se asigna al ID 1 automáticamente)
            Integer idPadreEfectivo = (request.idCasoPadre() != null) ? request.idCasoPadre() : 1;

            // Validación de seguridad: Evitar bucle infinito
            if (id.equals(idPadreEfectivo)) {
                throw new IllegalArgumentException("Un caso no puede ser su propio padre");
            }

            Caso padre = casoRepository.findById(idPadreEfectivo)
                    .orElseThrow(() -> new EntityNotFoundException("Caso padre no encontrado (ID: " + idPadreEfectivo + ")"));
            oldCaso.setCasoPadre(padre);

            Caso casoActualizado = casoRepository.save(oldCaso);

            // 7. Registro automático en el historial
            registrarCambioEnHistorial(casoActualizado, request.idPersonaCreador(),
                    "Actualización de datos. Estado anterior: " + estadoAnterior + ". Nuevo padre ID: " + idPadreEfectivo);
            return casoMapper.toResponse(casoActualizado);

        }).orElseThrow(() -> new EntityNotFoundException("Caso no encontrado con ID: " + id));
    }

    private void registrarCambioEnHistorial(Caso caso, Integer idPersona, String descripcion) {
        Historial_Cambio historial = new Historial_Cambio();
        historial.setCaso(caso);
        historial.setPersona(personaRepository.getReferenceById(idPersona));
        historial.setDescripcion(descripcion);
        historial.setFecha(LocalDate.now());
        historialCambioRepository.save(historial);
    }

    @Override
    public void desactivarCaso(Integer id) {
        if (id == 1) throw new IllegalArgumentException("No se puede desactivar el caso raíz del sistema");

        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
        caso.setActivo(false);
        casoRepository.save(caso);
    }
}