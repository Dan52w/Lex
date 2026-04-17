package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface SubtipoCasoService {
    SubtipoCasoResponseDTO guardar(SubtipoCasoRequestDTO request);
    List<SubtipoCasoResponseDTO> listarTodo();
    List<SubtipoCasoResponseDTO> listarPorTipo(Integer idTipoCaso);
    Optional<SubtipoCasoResponseDTO> buscarPorId(Integer id);
    SubtipoCasoResponseDTO actualizar(Integer id, SubtipoCasoRequestDTO request);
    void eliminar(Integer id);
}