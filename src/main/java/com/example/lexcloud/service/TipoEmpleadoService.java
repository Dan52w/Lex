package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoEmpleadoService {
    TipoEmpleadoResponseDTO guardar(TipoEmpleadoRequestDTO request);
    List<TipoEmpleadoResponseDTO> listarPorNombre(String nombre);
    List<TipoEmpleadoResponseDTO> listarTodos();
    Optional<TipoEmpleadoResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoEmpleadoResponseDTO> buscarPorId(Integer id);
    TipoEmpleadoResponseDTO actualizar(Integer id, TipoEmpleadoRequestDTO request);
    void eliminar(Integer id);
}
