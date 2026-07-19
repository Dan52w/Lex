package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.DiscapacidadRequestDTO;
import com.example.lexcloud.dto.response.DiscapacidadResponseDTO;

import java.util.List;
import java.util.Optional;

public interface DiscapacidadService {
    DiscapacidadResponseDTO guardar(DiscapacidadRequestDTO request);
    List<DiscapacidadResponseDTO> listarPorNombre(String nombre);
    List<DiscapacidadResponseDTO> listarTodos();
    Optional<DiscapacidadResponseDTO> buscarPorNombre(String nombre);
    Optional<DiscapacidadResponseDTO> buscarPorId(Integer id);
    DiscapacidadResponseDTO actualizar(Integer id, DiscapacidadRequestDTO request);
    void eliminar(Integer id);
}
