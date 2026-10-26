package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;

import java.util.List;

public interface HistorialCambioService {
    HistorialCambioResponseDTO guardar(HistorialCambioRequestDTO request);
    List<HistorialCambioResponseDTO> listarPorCaso(Integer idCaso);
    org.springframework.data.domain.Page<HistorialCambioResponseDTO> listarPorCaso(Integer idCaso, org.springframework.data.domain.Pageable pageable);
}
