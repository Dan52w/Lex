package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.HistorialCambioMapper;
import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
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
import java.util.List;

@Service
@Transactional
public class HistorialCambioServiceImpl implements HistorialCambioService {

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
    public HistorialCambioResponseDTO guardar(HistorialCambioRequestDTO request) {
        // SVC-4: solo se registra historial en casos a los que se tiene acceso.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        Historial_Cambio historial = historialMapper.toEntity(request);

        // Seteamos la fecha actual automáticamente si no viene en el DTO
        if (historial.getFecha() == null) {
            historial.setFecha(LocalDate.now());
        }
        historial.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado")));
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
