package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.TipoIdentificacionRequestDTO;
import com.example.lexcloud.dto.response.TipoIdenficacionResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TipoIdentificacionService {
    TipoIdenficacionResponseDTO guardar(TipoIdentificacionRequestDTO request);
    List<TipoIdenficacionResponseDTO> listarPorNombre(String nombre);
    List<TipoIdenficacionResponseDTO> listarTodos();
    Optional<TipoIdenficacionResponseDTO> buscarPorNombre(String nombre);
    Optional<TipoIdenficacionResponseDTO> buscarPorId(Integer id);
    TipoIdenficacionResponseDTO actualizar(Integer id, TipoIdentificacionRequestDTO request);
    void eliminar(Integer id);
    List<TipoIdenficacionResponseDTO> listarTodos(int page, int size);
    List<TipoIdenficacionResponseDTO> listarPorNombre(String nombre, int page, int size);
}
