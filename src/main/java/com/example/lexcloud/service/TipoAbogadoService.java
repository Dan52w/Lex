package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.TipoAbogadoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoAbogadoService {
    TipoAbogadoResponseDTO guardar(TipoAbogadoRequestDTO request);
    List<TipoAbogadoResponseDTO> listarTodo();
    List<TipoAbogadoResponseDTO> listarPorNombre(String nombre);
    Optional<TipoAbogadoResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoAbogadoResponseDTO> buscarPorId(Integer id);
    TipoAbogadoResponseDTO actualizar(Integer id, TipoAbogadoRequestDTO request);
    void eliminar(Integer id);
}
