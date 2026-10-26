package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes);
    List<DocumentoResponseDTO> listarPorCaso(Integer idCaso);
    DocumentoResponseDTO buscarPorId(Integer id);
    void eliminar(Integer id);
    /**
     * P0-B5: valida acceso al caso y devuelve su firma, para que el
     * controlador construya la carpeta de almacenamiento desde el servidor
     * (nunca desde el NIT que envía el cliente).
     */
    Integer firmaDeCaso(Integer idCaso);
}