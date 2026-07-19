package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.EstratoEconomicoRequestDTO;
import com.example.lexcloud.dto.response.EstratoEconomicoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface EstratoEconomicoService {
    EstratoEconomicoResponseDTO guardar(EstratoEconomicoRequestDTO request);
    List<EstratoEconomicoResponseDTO> listarPorNombre(String nombre);
    List<EstratoEconomicoResponseDTO> listarTodos();
    Optional<EstratoEconomicoResponseDTO> buscarPorNombre(String nombre);
    Optional<EstratoEconomicoResponseDTO> buscarPorId(Integer id);
    EstratoEconomicoResponseDTO actualizar(Integer id, EstratoEconomicoRequestDTO request);
    void eliminar(Integer id);
}
