package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;

import java.util.Optional;

public interface PersonaService {
    PersonaResponseDTO guardar(PersonaRequestDTO request);
    Optional<PersonaResponseDTO> buscarPorCc(String cc);
    Optional<PersonaResponseDTO> buscarPorIdActivo(Integer id);
    Optional<PersonaResponseDTO> buscarPorId(Integer id);
    PersonaResponseDTO actualizar(Integer id, PersonaRequestDTO requestDTO);
    boolean existeCc(String cc);
    void desactivar(Integer id);
    /** P0-B3: reactivación EXPLÍCITA y solo-admin (nunca al editar). */
    void reactivar(Integer id);
}
