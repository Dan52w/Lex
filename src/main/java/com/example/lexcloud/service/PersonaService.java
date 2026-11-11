package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface PersonaService {
    PersonaResponseDTO guardar(PersonaRequestDTO request);
    Optional<PersonaResponseDTO> buscarPorCc(String cc);
    Optional<PersonaResponseDTO> buscarPorIdActivo(Integer id);
    Optional<PersonaResponseDTO> buscarPorId(Integer id);
    // M1: listado paginado (sin parámetros → primera página, tamaño por defecto).
    List<PersonaResponseDTO> listarTodo();
    List<PersonaResponseDTO> listarTodo(int page, int size);
    PersonaResponseDTO actualizar(Integer id, PersonaRequestDTO requestDTO);
    boolean existeCc(String cc);
    void desactivar(Integer id);
    void reactivar(Integer id);
}
