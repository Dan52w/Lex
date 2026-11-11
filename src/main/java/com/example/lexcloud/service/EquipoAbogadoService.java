package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;

import java.util.List;

public interface EquipoAbogadoService {
    EquipoAbogadoResponseDTO crearAsignacion(EquipoAbogadoRequestDTO requestDTO);
    EquipoAbogadoResponseDTO obtenerPorId(Integer id);
    List<EquipoAbogadoResponseDTO> listarTodosActivos();
    List<EquipoAbogadoResponseDTO> listarTodosActivos(int page, int size);
    List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado);
    List<EquipoAbogadoResponseDTO> listarPorAbogado(Integer idAbogado, int page, int size);
    List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado);
    List<EquipoAbogadoResponseDTO> listarActivosPorAbogado(Integer idAbogado, int page, int size);
    List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona);
    List<EquipoAbogadoResponseDTO> listarPorPersona(Integer idPersona, int page, int size);
    List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion);
    List<EquipoAbogadoResponseDTO> listarPorTipoRelacion(Integer idTipoRelacion, int page, int size);
    List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion);
    List<EquipoAbogadoResponseDTO> listarActivosPorTipoRelacion(Integer idTipoRelacion, int page, int size);
    void desactivarAsignacion(Integer idAbogado, Integer idPersona);
    void eliminarAsignacion(Integer id);
}
