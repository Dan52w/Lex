package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.GeneroRequestDTO;
import com.example.lexcloud.dto.response.GeneroResponseDTO;

import java.util.List;
import java.util.Optional;

public interface GeneroService {
    GeneroResponseDTO guardar(GeneroRequestDTO request);
    List<GeneroResponseDTO> listarPorNombre(String nombre);
    List<GeneroResponseDTO> listarTodos();
    Optional<GeneroResponseDTO> buscarPorNombre(String nombre);
    Optional<GeneroResponseDTO> buscarPorId(Integer id);
    GeneroResponseDTO actualizar(Integer id, GeneroRequestDTO request);
    void eliminar(Integer id);
}
