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

import java.time.LocalDateTime;
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
    @Transactional
    public DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes) {
        // 1. Mapeo inicial de los datos básicos del DTO a la Entidad
        Documento documento = documentoMapper.toEntity(request);

        // 2. Establecer metadatos técnicos del archivo
        documento.setDireccion(direccionArchivo);
        documento.setTamanio(pesoEnBytes); // El campo que definimos como BIGINT

        // 3. Vincular con el Caso
        // Usamos findById para asegurar que el caso existe antes de intentar guardar
        documento.setCaso(casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("No se puede subir el documento: Caso no encontrado con ID: " + request.idCaso())));

        // 4. Vincular con el Tipo de Documento (Poder, Demanda, Prueba, etc.)
        documento.setTipo_Documento(tipoDocumentoRepository.findById(request.idTipoDocumento())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de documento no válido")));

        // 6. Persistencia
        Documento documentoGuardado = documentoRepository.save(documento);

        // 7. Retornar la respuesta mapeada
        return documentoMapper.toResponse(documentoGuardado);
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