package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes);
    List<DocumentoResponseDTO> listarPorCaso(Integer idCaso);
    List<DocumentoResponseDTO> listarPorCaso(Integer idCaso, int page, int size);
    DocumentoResponseDTO buscarPorId(Integer id);
    void eliminar(Integer id);
}