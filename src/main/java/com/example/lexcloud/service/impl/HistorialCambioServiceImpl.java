package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.HistorialCambioMapper;
import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Historial_Cambio;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.HistorialCambio_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.HistorialCambioService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HistorialCambioServiceImpl implements HistorialCambioService {

    /** Zona horaria en la que se fija la fecha de auditoría (M19). */
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Bogota");

    private final HistorialCambio_Repository historialRepository;
    private final Caso_Repository casoRepository;
    private final Persona_Repository personaRepository;
    private final HistorialCambioMapper historialMapper;
    private final ContextoSeguridad contextoSeguridad;

    public HistorialCambioServiceImpl(HistorialCambio_Repository historialRepository,
                                      Caso_Repository casoRepository,
                                      Persona_Repository personaRepository,
                                      HistorialCambioMapper historialMapper,
                                      ContextoSeguridad contextoSeguridad) {
        this.historialRepository = historialRepository;
        this.casoRepository = casoRepository;
        this.personaRepository = personaRepository;
        this.historialMapper = historialMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public HistorialCambioResponseDTO guardar(HistorialCambioRequestDTO request) {
        // SVC-4: solo se registra historial en casos a los que se tiene acceso.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        Historial_Cambio historial = historialMapper.toEntity(request);

        // M19: la fecha de auditoría la fija SIEMPRE el servidor (la fecha
        // que venga en el DTO se ignora: permitirla sería falsificar la
        // auditoría). La entidad guarda LocalDate.
        historial.setFecha(LocalDate.now(ZONA_HORARIA));

        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // M9: no se registra historial en un caso inactivo.
        if (!Boolean.TRUE.equals(caso.getActivo())) {
            throw new IllegalArgumentException("No se puede registrar historial en un caso inactivo.");
        }
        historial.setCaso(caso);
        // El autor del registro es el usuario autenticado, no el idPersona que
        // viene en el DTO (el front lo envía, pero es un dato del cliente:
        // aceptar otro id permitiría firmar el historial a nombre de otra persona).
        historial.setPersona(personaRepository.findById(contextoSeguridad.idPersonaAutenticada())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada")));

        return historialMapper.toResponse(historialRepository.save(historial));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialCambioResponseDTO> listarPorCaso(Integer idCaso) {
        return listarPorCaso(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialCambioResponseDTO> listarPorCaso(Integer idCaso, int page, int size) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        // Obtenemos los cambios del más reciente al más antiguo
        return historialMapper.toResponseList(
                historialRepository.findByCasoId(idCaso, Paginacion.crear(page, size))
        );
    }
}
