package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoRelacionEquipoRequestDTO;
import com.example.lexcloud.dto.response.TipoRelacionEquipoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoRelacionEquipoService {
    TipoRelacionEquipoResponseDTO guardar(TipoRelacionEquipoRequestDTO request);
    List<TipoRelacionEquipoResponseDTO> listarPorNombre(String nombre);
    List<TipoRelacionEquipoResponseDTO> listarTodos();
    Optional<TipoRelacionEquipoResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoRelacionEquipoResponseDTO> buscarPorId(Integer id);
    TipoRelacionEquipoResponseDTO actualizar(Integer id, TipoRelacionEquipoRequestDTO request);
    void eliminar(Integer id);
}
