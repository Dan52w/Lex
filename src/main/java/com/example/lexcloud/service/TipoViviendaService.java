package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoViviendaRequestDTO;
import com.example.lexcloud.dto.response.TipoViviendaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoViviendaService {
    TipoViviendaResponseDTO guardar(TipoViviendaRequestDTO request);
    List<TipoViviendaResponseDTO> listarPorNombre(String nombre);
    List<TipoViviendaResponseDTO> listarTodos();
    Optional<TipoViviendaResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoViviendaResponseDTO> buscarPorId(Integer id);
    TipoViviendaResponseDTO actualizar(Integer id, TipoViviendaRequestDTO request);
    void eliminar(Integer id);
}
