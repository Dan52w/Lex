package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.VinculoRequestDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface VinculoService {
    VinculoResponseDTO guardar(VinculoRequestDTO request);
    List<VinculoResponseDTO> listarTodo();
    List<VinculoResponseDTO> listarPorNombre(String nombre);
    Optional<VinculoResponseDTO> buscarPorNombre(String nombre);
    Optional<VinculoResponseDTO> buscarPorId(Integer id);
    VinculoResponseDTO actualizar(Integer id, VinculoRequestDTO request);
    void eliminar(Integer id);
}
