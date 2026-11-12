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
    /**
     * Agendas del alcance del usuario autenticado, en rango de fechas
     * (la que muestra el dashboard): súper admin y admin bootstrap
     * (sin firma resoluble) ven TODAS las firmas; admin CON firma, solo
     * su firma; abogado u otro rol CON firma, solo su propia agenda
     * (sin perfil de abogado: lista vacía, nunca error); no-admin SIN
     * firma resoluble: lista vacía (fail-closed).
     */
    List<AgendaResponseDTO> listarMisAgendas(LocalDateTime inicio, LocalDateTime fin);
    List<AgendaResponseDTO> listarMisAgendas(LocalDateTime inicio, LocalDateTime fin, int page, int size);
    Optional<AgendaResponseDTO> buscarPorId(Integer id);
    AgendaResponseDTO actualizar(Integer id, AgendaRequestDTO request);
    void desactivarAgenda(Integer id);
}
