package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    ClienteResponseDTO guardar(ClienteRequestDTO request);
    List<ClienteResponseDTO> listarFirma(Integer idFirma);
    List<ClienteResponseDTO> listarNombre(Integer idFirma, String nombre);
    Optional<ClienteResponseDTO> buscarPorCc(String cc, Integer idFirma);
    Optional<ClienteResponseDTO> buscarPorIdPersona(Integer idPersona);
    Optional<ClienteResponseDTO> buscarPorId(Integer id);
    ClienteResponseDTO actualizar(Integer id, ClienteRequestDTO request);
    void desactivarCliente(Integer id);
}
