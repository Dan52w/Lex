package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface VinculoCasoService {
    VinculoCasoResponseDTO vincular(VinculoCasoRequestDTO request);
    List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso);
    List<VinculoCasoResponseDTO> listarPersonalAsignado(Integer idCaso, int page, int size);
    List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona);
    List<VinculoCasoResponseDTO> listarMisCasos(Integer idPersona, int page, int size);
    List<VinculoCasoResponseDTO> listarPendientesAutorizacion();
    List<VinculoCasoResponseDTO> listarPendientesAutorizacion(int page, int size);
    Optional<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(Integer idCaso);
    Optional<VinculoCasoResponseDTO> obtenerCliente(Integer idCaso);
    void autorizarAcceso(Integer idVinculoCaso);
    void desautorizarAcceso(Integer idVinculoCaso);
    void rechazarAcceso(Integer idVinculoCaso);
    void eliminarVinculo(Integer id);

    // Métodos de validación de seguridad
    boolean tieneAcceso(Integer idPersona, Integer idCaso);
}