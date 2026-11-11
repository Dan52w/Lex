package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    ClienteResponseDTO guardar(ClienteRequestDTO request);
    List<ClienteResponseDTO> listarFirma(Integer idFirma);
    List<ClienteResponseDTO> listarFirma(Integer idFirma, int page, int size);
    // P0-B2: clientes de la firma del usuario autenticado. Admin: TODOS
    // los clientes activos (todas las firmas). Sin firma: lista vacía.
    List<ClienteResponseDTO> listarMiFirma(int page, int size);
    // P0-B2: TODOS los clientes activos (todas las firmas). Solo admin.
    List<ClienteResponseDTO> listarTodosActivos(int page, int size);
    List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro);
    List<ClienteResponseDTO> buscarClientes(Integer idFirma, String filtro, int page, int size);
    Optional<ClienteResponseDTO> buscarPorIdentificacion(String identificacion, Integer idFirma);
    Optional<ClienteResponseDTO> buscarPorId(Integer id, Integer idFirma);
    ClienteResponseDTO actualizar(Integer id, Integer idFirma, ClienteRequestDTO request);
    void desactivarCliente(Integer id, Integer idFirma);
    // B3: reactivar un cliente dado de baja (solo admin, misma firma).
    void reactivarCliente(Integer id, Integer idFirma);
}