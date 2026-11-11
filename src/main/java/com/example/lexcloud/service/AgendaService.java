package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.AgendaRequestDTO;
import com.example.lexcloud.dto.response.AgendaResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendaService {
    AgendaResponseDTO guardar(AgendaRequestDTO request);
    List<AgendaResponseDTO> listarAbogado(Integer idAbogado);
    List<AgendaResponseDTO> listarAbogado(Integer idAbogado, int page, int size);
    List<AgendaResponseDTO> listarCaso(Integer idCaso);
    List<AgendaResponseDTO> listarCaso(Integer idCaso, int page, int size);
    List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin);
    List<AgendaResponseDTO> listarAbogadoFechaRange(Integer idAbogado, LocalDateTime inicio, LocalDateTime fin, int page, int size);
    List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin);
    List<AgendaResponseDTO> listarCasoFechaRange(Integer idCaso, LocalDateTime inicio, LocalDateTime fin, int page, int size);
    List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado);
    List<AgendaResponseDTO> listarProximaCitas(Integer idAbogado, int page, int size);
    Optional<AgendaResponseDTO> buscarPorId(Integer id);
    AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request);
    void desactivarAgenda(Integer id);
}
