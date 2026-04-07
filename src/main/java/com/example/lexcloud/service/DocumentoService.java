package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo);
    List<DocumentoResponseDTO> listarPorCaso(Integer idCaso);
    DocumentoResponseDTO buscarPorId(Integer id);
    void eliminar(Integer id);
}