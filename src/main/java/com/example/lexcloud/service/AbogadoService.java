package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;

import java.util.List;
import java.util.Optional;

public interface AbogadoService {
    AbogadoResponseDTO guardar(AbogadoRequestDTO request);
    Optional<AbogadoResponseDTO> buscarPorId(Integer id);
    List<AbogadoResponseDTO> listarTodos(Integer idFirma);
    List<AbogadoResponseDTO> listarTodos(Integer idFirma, int page, int size);
    List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad);
    List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad, int page, int size);
    List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre);
    List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre, int page, int size);
    Optional<AbogadoResponseDTO> buscarPorCc(String cc, Integer idFirma);
    Optional<AbogadoResponseDTO> buscarPorPersonaId(Integer id);
    AbogadoResponseDTO actualizar(Integer id, AbogadoRequestDTO request);
    void desactivarAbogado(Integer id);
    void reactivarAbogado(Integer id);
}
