package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    ClienteResponseDTO guardar(ClienteRequestDTO request);
    List<ClienteResponseDTO> listarFirma(Integer idFirma);
    org.springframework.data.domain.Page<ClienteResponseDTO> listarFirma(Integer idFirma, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro, org.springframework.data.domain.Pageable pageable);
    List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro);
    Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma);
    Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma);
    ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request);
    void desactivarCliente(Integer id, Integer idFirma);
}