package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoDocumentoRequestDTO;
import com.example.lexcloud.dto.response.TipoDocumentoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoDocumentoService {
    TipoDocumentoResponseDTO guardar(TipoDocumentoRequestDTO request);
    List<TipoDocumentoResponseDTO> listarTodo();
    List<TipoDocumentoResponseDTO> listarPorNombre(String nombre);
    Optional<TipoDocumentoResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoDocumentoResponseDTO> buscarPorId(Integer id);
    TipoDocumentoResponseDTO actualizar(Integer id, TipoDocumentoRequestDTO request);
    void eliminar(Integer id);
}
