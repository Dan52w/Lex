package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.HistorialCambioMapper;
import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
import com.example.lexcloud.entidades.Historial_Cambio;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.HistorialCambio_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.HistorialCambioService;
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
    private final CurrentUser currentUser;

    public HistorialCambioServiceImpl(HistorialCambio_Repository historialRepository,
                                      Caso_Repository casoRepository,
                                      Persona_Repository personaRepository,
                                      HistorialCambioMapper historialMapper,
                                      CurrentUser currentUser) {
        this.historialRepository = historialRepository;
        this.casoRepository = casoRepository;
        this.personaRepository = personaRepository;
        this.historialMapper = historialMapper;
        this.currentUser = currentUser;
    }

    @Override
    public HistorialCambioResponseDTO guardar(HistorialCambioRequestDTO request) {
        // P0-B2/A3: caso accesible y autor = token (nunca idPersona del DTO).
        currentUser.requireCaso(request.idCaso());
        Historial_Cambio historial = historialMapper.toEntity(request);

        // Seteamos la fecha actual automáticamente si no viene en el DTO
        if (historial.getFecha() == null) {
            historial.setFecha(LocalDate.now());
        }
        historial.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado")));
        historial.setPersona(personaRepository.findById(currentUser.personaId())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada")));

        return historialMapper.toResponse(historialRepository.save(historial));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialCambioResponseDTO> listarPorCaso(Integer idCaso) {
        currentUser.requireCaso(idCaso);
        // Obtenemos los cambios del más reciente al más antiguo
        return historialMapper.toResponseList(
                historialRepository.findByCasoId(idCaso)
        );
    }
}