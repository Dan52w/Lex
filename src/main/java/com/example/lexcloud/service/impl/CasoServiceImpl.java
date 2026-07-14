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
    private final Cliente_Repository clienteRepository;
    private final HistorialCambio_Repository historialCambioRepository;
    private final Firma_Repository firmaRepository;
    private final CasoMapper casoMapper;

    public CasoServiceImpl(Caso_Repository casoRepository,
                           Estado_Repository estadoRepository,
                           SubtipoCaso_Repository subtipoRepository,
                           Persona_Repository personaRepository,
                           VinculoCaso_Repository vinculoCasoRepository,
                           HistorialCambio_Repository historialCambioRepository,
                           Vinculo_Repository vinculoRepository,
                           Cliente_Repository clienteRepository,
                           Firma_Repository firmaRepository,
                           CasoMapper casoMapper) {
        this.casoRepository = casoRepository;
        this.estadoRepository = estadoRepository;
        this.subtipoRepository = subtipoRepository;
        this.personaRepository = personaRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.historialCambioRepository = historialCambioRepository;
        this.vinculoRepository = vinculoRepository;
        this.clienteRepository = clienteRepository;
        this.firmaRepository = firmaRepository;
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

        caso.setFirma(firmaRepository.findById(request.idFirma())
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada")));

        Caso casoGuardado = casoRepository.save(caso);

        // Obtenemos la referencia directa del cliente
        Cliente cliente = clienteRepository.getReferenceById(request.idCliente());

        // 3. Vínculo automático vinculando directamente al CLIENTE (apuntando a la nueva FK)
        crearVinculo(casoGuardado, request.idPersonaCreador(), cliente, true);

        return casoMapper.toResponse(casoGuardado);
    }

    private void crearVinculo(Caso caso, Integer idPersona, Cliente cliente, boolean autorizado) {
        Persona persona = personaRepository.getReferenceById(idPersona);
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
        return casoRepository.findCasosAutorizadosPorPersona(idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente) {
        return casoRepository.findCasosAutorizadosPorAbogadoYCliente(idPersonaFirma, idCliente).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona) {
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
    public List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso) {
        return casoRepository.findByCasoPadreId(idCaso).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // NUEVAS IMPLEMENTACIONES: RADICADOS Y SÚPER BUSCADOR
    // =========================================================================

    @Override
    public CasoResponseDTO buscarPorRadicadoInterno(String radicadoInterno, Integer idPersona) {
        return casoRepository.findByRadicadoInternoAndPersona(radicadoInterno, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso con radicado interno '" + radicadoInterno + "' no encontrado o no autorizado"));
    }

    @Override
    public CasoResponseDTO buscarPorRadicadoJudicial(String radicadoJudicial, Integer idPersona) {
        return casoRepository.findByRadicadoJudicialAndPersona(radicadoJudicial, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso con radicado judicial '" + radicadoJudicial + "' no encontrado o no autorizado"));
    }

    @Override
    public List<CasoResponseDTO> superBuscador(String termino, Integer idPersona) {
        return casoRepository.superBuscador(termino, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================

    @Override
    public List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona) {
        return casoRepository.findByClienteIdAndPersona(idCliente, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona) {
        return casoRepository.buscarPorClienteNombreOIdentificacion(termino, idPersona).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CasoResponseDTO buscarPorId(Integer id) {
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
        if (id == 1) {
            throw new EntityNotFoundException("No se puede modificar el caso raíz");
        }

        return casoRepository.findById(id).map(oldCaso -> {
            String estadoAnterior = (oldCaso.getEstado() != null) ? oldCaso.getEstado().getNombre() : "Sin estado";
            String subtipoCasoAnterior = (oldCaso.getSubtipo_Caso() != null) ? oldCaso.getSubtipo_Caso().getNombre() : "Sin definir";
            String nombreCasoAnterior = (oldCaso.getTitulo() != null) ? oldCaso.getTitulo() : "Sin titulo";

            oldCaso.setTitulo(request.titulo());
            oldCaso.setDescripcion(request.descripcion());
            oldCaso.setEstado(estadoRepository.findById(request.idEstado())
                    .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado")));
            oldCaso.setSubtipo_Caso(subtipoRepository.findById(request.idSubtipo())
                    .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada")));

            Caso casoActualizado = casoRepository.save(oldCaso);

            registrarCambioEnHistorial(casoActualizado, request.idPersonaCreador(),
                    "Actualización de datos. Estado anterior: " + estadoAnterior + ". Especialidad anterior: " + subtipoCasoAnterior + ". Titulo anterior: " + nombreCasoAnterior);
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