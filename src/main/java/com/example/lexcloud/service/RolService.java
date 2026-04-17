package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.RolRequestDTO;
import com.example.lexcloud.dto.response.RolResponseDTO;

import java.util.List;
import java.util.Optional;

public interface RolService {
    RolResponseDTO guardar(RolRequestDTO request);
    List<RolResponseDTO> listarTodo();
    List<RolResponseDTO> listarPorNombre(String nombre);
    Optional<RolResponseDTO> buscarPorNombre(String nombre);
    Optional<RolResponseDTO> buscarPorId(Integer id);
    RolResponseDTO actualizar(Integer id, RolRequestDTO request);
    void eliminar(Integer id);
}
