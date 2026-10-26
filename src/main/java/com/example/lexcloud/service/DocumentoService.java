package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes);
    List<DocumentoResponseDTO> listarPorCaso(Integer idCaso);
    org.springframework.data.domain.Page<DocumentoResponseDTO> listarPorCaso(Integer idCaso, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<DocumentoResponseDTO> listarPapelera(Integer firmaId, org.springframework.data.domain.Pageable pageable);
    void restaurar(Integer id);
    void eliminarDefinitivo(Integer id);
    DocumentoResponseDTO buscarPorId(Integer id);
    void eliminar(Integer id);
    /**
     * P0-B5: valida acceso al caso y devuelve su firma, para que el
     * controlador construya la carpeta de almacenamiento desde el servidor
     * (nunca desde el NIT que envía el cliente).
     */
    Integer firmaDeCaso(Integer idCaso);
}