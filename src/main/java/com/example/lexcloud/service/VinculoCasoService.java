package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface VinculoCasoService {
    VinculoCasoResponseDTO vincular(VinculoCasoRequestDTO request);
    List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso);
    List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona);
    List<VinculoCasoResponseDTO> listarPendientesAutorizacion();
    Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso);
    Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso);
    void autorizarAcceso(Integer idVinculoCaso);
    void eliminarVinculo(Integer id);

    // Métodos de validación de seguridad
    boolean tieneAcceso(Integer idPersona, Integer idCaso);
}