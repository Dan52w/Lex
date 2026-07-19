package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.EstadoCivilRequestDTO;
import com.example.lexcloud.dto.response.EstadoCivilResponseDTO;

import java.util.List;
import java.util.Optional;

public interface EstadoCivilService {
    EstadoCivilResponseDTO guardar(EstadoCivilRequestDTO request);
    List<EstadoCivilResponseDTO> listarPorNombre(String nombre);
    List<EstadoCivilResponseDTO> listarTodos();
    Optional<EstadoCivilResponseDTO> buscarPorNombre(String nombre);
    Optional<EstadoCivilResponseDTO> buscarPorId(Integer id);
    EstadoCivilResponseDTO actualizar(Integer id, EstadoCivilRequestDTO request);
    void eliminar(Integer id);
}
