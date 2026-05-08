package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoClienteRequestDTO;
import com.example.lexcloud.dto.response.TipoClienteResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoClienteService {
    TipoClienteResponseDTO guardar(TipoClienteRequestDTO request);
    List<TipoClienteResponseDTO> listarTodo();
    List<TipoClienteResponseDTO> listarPorNombre(String nombre);
    Optional<TipoClienteResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoClienteResponseDTO> buscarPorId(Integer id);
    TipoClienteResponseDTO actualizar(Integer id, TipoClienteRequestDTO request);
    void eliminar(Integer id);
}
