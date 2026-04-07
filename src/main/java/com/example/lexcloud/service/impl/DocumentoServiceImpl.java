package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.DocumentoMapper;
import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.entidades.Documento;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Documento_Repository;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.service.DocumentoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentoServiceImpl implements DocumentoService {

    private final Documento_Repository documentoRepository;
    private final Caso_Repository casoRepository;
    private final TipoDocumento_Repository tipoDocumentoRepository;
    private final DocumentoMapper documentoMapper;

    public DocumentoServiceImpl(Documento_Repository documentoRepository,
                                Caso_Repository casoRepository,
                                TipoDocumento_Repository tipoDocumentoRepository,
                                DocumentoMapper documentoMapper) {
        this.documentoRepository = documentoRepository;
        this.casoRepository = casoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.documentoMapper = documentoMapper;
    }

    @Override
    public DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo) {
        // 1. Mapeo inicial
        Documento documento = documentoMapper.toEntity(request);

        // 2. Establecer la dirección física/lógica del archivo
        documento.setDireccion(direccionArchivo);

        // 3. Vincular con el Caso
        documento.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado")));

        // 4. Vincular con el Tipo de Documento
        documento.setTipo_Documento(tipoDocumentoRepository.findById(request.idTipoDocumento())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de documento no encontrado")));

        return documentoMapper.toResponse(documentoRepository.save(documento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarPorCaso(Integer idCaso) {
        return documentoRepository.findByCasoId(idCaso).stream()
                .map(documentoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO buscarPorId(Integer id) {
        return documentoRepository.findById(id)
                .map(documentoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
    }

    @Override
    public void eliminar(Integer id) {
        if (!documentoRepository.existsById(id)) {
            throw new EntityNotFoundException("Documento no encontrado");
        }
        documentoRepository.deleteById(id);
    }
}