package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    ClienteResponseDTO guardar(ClienteRequestDTO request);
    List<ClienteResponseDTO> listarFirma(Integer idFirma);
    List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro);
    Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma);
    Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma);
    ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request);
    void desactivarCliente(Integer id, Integer idFirma);
}