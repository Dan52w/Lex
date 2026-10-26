package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import java.util.List;

public interface CasoService {
    CasoResponseDTO guardar(CasoRequestDTO request);

    // Búsquedas seguras por persona
    List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona);
    org.springframework.data.domain.Page<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona, org.springframework.data.domain.Pageable pageable);
    List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente);
    List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona);
    List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona);
    List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona);
    List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo);
    List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso);

    // Nuevas búsquedas por radicados y Súper Buscador
    CasoResponseDTO buscarPorRadicadoInterno(String radicadoInterno, Integer idPersona);
    CasoResponseDTO buscarPorRadicadoJudicial(String radicadoJudicial, Integer idPersona);
    List<CasoResponseDTO> superBuscador(String termino, Integer idPersona);

    // Búsquedas por clientes
    List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona);
    List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona);

    // P1: casos de un miembro del equipo (vista equipo/[id]).
    // El id es el id del recurso ABOGADO, no el idPersona: el front
    // resolvía el abogado y luego pedía /casos/persona/{idPersona},
    // ruta que P0-B2 delimita al usuario del token. Sin paginación:
    // el front espera un array (casos.length, map) y el resto de
    // listados del proyecto devuelven List, no Page.
    List<CasoResponseDTO> listarCasosDeMiembro(Integer idAbogado);

    // Gestión
    CasoResponseDTO buscarPorId(Integer id);
    CasoResponseDTO actualizar(Integer id, CasoRequestDTO request);
    void desactivarCaso(Integer id);
}