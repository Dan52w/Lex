package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;

import java.util.List;

public interface EquipoAbogadoService {
    EquipoAbogadoResponseDTO crearAsignacion(EquipoAbogadoRequestDTO requestDTO);
    EquipoAbogadoResponseDTO obtenerPorId(Integer id);
    List<EquipoAbogadoResponseDTO> listarTodosActivos();
    List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado);
    List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado);
    List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona);
    List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion);
    List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion);
    void desactivarAsignacion(Integer idAbogado, Integer idPersona);
    void eliminarAsignacion(Integer id);
}