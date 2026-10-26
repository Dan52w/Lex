package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.AgendaRequestDTO;
import com.example.lexcloud.dto.response.AgendaResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentaService {
    AgendaResponseDTO guardar(AgendaRequestDTO request);
    List<AgendaResponseDTO> listarAbogado(Integer idAbogado);
    List<AgendaResponseDTO> listarCaso(Integer idCaso);
    org.springframework.data.domain.Page<AgendaResponseDTO> listarCaso(Integer idCaso, org.springframework.data.domain.Pageable pageable);
    List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin);
    org.springframework.data.domain.Page<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin, org.springframework.data.domain.Pageable pageable);
    List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin);
    org.springframework.data.domain.Page<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin, org.springframework.data.domain.Pageable pageable);
    List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado);
    Optional<AgendaResponseDTO> buscarPorId(Integer id);
    AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request);
    void desactivarAgenda(Integer id);
    /** P0-B2: citas del abogado del token en un rango (ruta /mias/rango). */
    List<AgendaResponseDTO> listarMisCitasRango(LocalDateTime inicio, LocalDateTime fin);
    org.springframework.data.domain.Page<AgendaResponseDTO> listarMisCitasRango(LocalDateTime inicio, LocalDateTime fin, org.springframework.data.domain.Pageable pageable);
}
