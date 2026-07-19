package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.NivelSalarialRequestDTO;
import com.example.lexcloud.dto.response.NivelSalarialResponseDTO;

import java.util.List;
import java.util.Optional;

public interface NivelSalarialService {
    NivelSalarialResponseDTO guardar(NivelSalarialRequestDTO request);
    List<NivelSalarialResponseDTO> listarPorRango(String rango);
    List<NivelSalarialResponseDTO> listarTodos();
    Optional<NivelSalarialResponseDTO> buscarPorRango(String rango);
    Optional<NivelSalarialResponseDTO> buscarPorId(Integer id);
    NivelSalarialResponseDTO actualizar(Integer id, NivelSalarialRequestDTO request);
    void eliminar(Integer id);
}
