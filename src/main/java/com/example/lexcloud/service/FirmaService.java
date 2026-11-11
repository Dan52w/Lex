package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface FirmaService {
    FirmaResponseDTO guardar(FirmaRequestDTO request);
    // P0-B2: TODAS las firmas (la entidad no tiene flag de activo).
    // Solo admin (selector de firma del front).
    List<FirmaResponseDTO> listarTodas(int page, int size);
    List<FirmaResponseDTO> listarPorNombre(String nombre);
    List<FirmaResponseDTO> listarPorNombre(String nombre, int page, int size);
    Optional<FirmaResponseDTO> buscarPorNit(String nit);
    Optional<FirmaResponseDTO> buscarPorId(Integer id);
    FirmaResponseDTO actualizar(Integer id, FirmaRequestDTO request);
}
