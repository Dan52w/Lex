package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface FirmaService {
    FirmaResponseDTO guardar(FirmaRequestDTO request);
    List<FirmaResponseDTO> listarPorNombre(String nombre);
    Optional<FirmaResponseDTO> buscarPorNit(String nit);
    Optional<FirmaResponseDTO> buscarPorId(Integer id);
    FirmaResponseDTO actualizar(Integer id, FirmaRequestDTO request);
}
