package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.GradoEscolaridadRequestDTO;
import com.example.lexcloud.dto.response.GradoEscolaridadResponseDTO;

import java.util.List;
import java.util.Optional;

public interface GradoEscolaridadService {
    GradoEscolaridadResponseDTO guardar(GradoEscolaridadRequestDTO request);
    List<GradoEscolaridadResponseDTO> listarPorNombre(String nombre);
    List<GradoEscolaridadResponseDTO> listarTodos();
    Optional<GradoEscolaridadResponseDTO> buscarPorNombre(String nombre);
    Optional<GradoEscolaridadResponseDTO> buscarPorId(Integer id);
    GradoEscolaridadResponseDTO actualizar(Integer id, GradoEscolaridadRequestDTO request);
    void eliminar(Integer id);
}
