package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import java.util.List;

public interface CasoService {
    CasoResponseDTO guardar(CasoRequestDTO request);

    // Búsquedas seguras por persona
    List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona);
    List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona);
    List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona);
    List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona);
    List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo);

    // Gestión
    CasoResponseDTO buscarPorId(Integer id);
    CasoResponseDTO actualizar(Integer id, CasoRequestDTO request);
    void desactivarCaso(Integer id);
}