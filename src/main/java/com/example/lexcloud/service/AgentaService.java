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
    List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin);
    List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin);
    List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado);
    Optional<AgendaResponseDTO> buscarPorId(Integer id);
    AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request);
    void desactivarAgenda(Integer id);
}
