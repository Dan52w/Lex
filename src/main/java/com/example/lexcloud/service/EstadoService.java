package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.EstadoRequestDTO;
import com.example.lexcloud.dto.response.EstadoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface EstadoService {
    EstadoResponseDTO guardar(EstadoRequestDTO request);
    List<EstadoResponseDTO> listarPorNombre(String nombre);
    List<EstadoResponseDTO> listarTodos();
    Optional<EstadoResponseDTO> buscarPorNombre(String nombre);
    Optional<EstadoResponseDTO> buscarPorId(Integer id);
    EstadoResponseDTO actualizar(Integer id, EstadoRequestDTO request);
    void eliminar(Integer id);
}
