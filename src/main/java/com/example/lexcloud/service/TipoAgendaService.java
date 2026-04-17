package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoAgentaResquestDTO;
import com.example.lexcloud.dto.response.TipoAgendaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoAgendaService {
    TipoAgendaResponseDTO guardar(TipoAgentaResquestDTO request);
    List<TipoAgendaResponseDTO> listarTodo();
    List<TipoAgendaResponseDTO> listarPorNombre(String nombre);
    Optional<TipoAgendaResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoAgendaResponseDTO> buscarPorId(Integer id);
    TipoAgendaResponseDTO actualizar(Integer id, TipoAgentaResquestDTO request);
    void eliminar(Integer id);
}
