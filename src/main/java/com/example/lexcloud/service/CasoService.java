package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import java.util.List;

public interface CasoService {
    CasoResponseDTO guardar(CasoRequestDTO request);

    // Búsquedas seguras por persona
    // (las variantes con page/size usan las consultas paginadas del repositorio;
    // las sin parámetros devuelven la primera página, tamaño máximo 100)
    List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona);
    List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona, int page, int size);
    List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente);
    List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente, int page, int size);
    List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona);
    List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona, int page, int size);
    List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona);
    List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona, int page, int size);
    List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona);
    List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona, int page, int size);
    List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo);
    List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo, int page, int size);
    List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso);

    // Nuevas búsquedas por radicados y Súper Buscador
    CasoResponseDTO buscarPorRadicadoInterno(String radicadoInterno, Integer idPersona);
    CasoResponseDTO buscarPorRadicadoJudicial(String radicadoJudicial, Integer idPersona);
    List<CasoResponseDTO> superBuscador(String termino, Integer idPersona);
    List<CasoResponseDTO> superBuscador(String termino, Integer idPersona, int page, int size);

    // Búsquedas por clientes
    List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona);
    List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona, int page, int size);
    List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona);
    List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona, int page, int size);

    // Gestión
    CasoResponseDTO buscarPorId(Integer id);
    CasoResponseDTO actualizar(Integer id, CasoRequestDTO request);
    void desactivarCaso(Integer id);
}