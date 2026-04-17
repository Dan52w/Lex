package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoCasoService {
    TipoCasoResponseDTO guardar(TipoCasoRequestDTO request);
    List<TipoCasoResponseDTO> listarTodo();
    List<TipoCasoResponseDTO> listarPorNombre(String nombre);
    Optional<TipoCasoResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoCasoResponseDTO> buscarPorId(Integer id);
    TipoCasoResponseDTO actualizar(Integer id, TipoCasoRequestDTO request);
    void eliminar(Integer id);
}
