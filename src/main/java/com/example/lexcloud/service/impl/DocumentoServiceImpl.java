package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.DocumentoMapper;
import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.entidades.Documento;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Documento_Repository;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.service.DocumentoService;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.FileStorageService;
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
    private final FileStorageService fileStorageService;
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;

    public DocumentoServiceImpl(Documento_Repository documentoRepository,
                                Caso_Repository casoRepository,
                                TipoDocumento_Repository tipoDocumentoRepository,
                                DocumentoMapper documentoMapper,
                                FileStorageService fileStorageService,
                                CurrentUser currentUser,
                                AuditoriaService auditoriaService) {
        this.documentoRepository = documentoRepository;
        this.casoRepository = casoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.documentoMapper = documentoMapper;
        this.fileStorageService  = fileStorageService;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes) {
        // P0-B2: el caso debe ser accesible para el token (firma + vínculo
        // autorizado, o admin). El nitFirma del DTO se ignora por completo.
        currentUser.requireCaso(request.idCaso());
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
        currentUser.requireCaso(idCaso);
        // P1-B7: la papelera no aparece en el listado normal del caso.
        return documentoRepository.findByCasoId(idCaso).stream()
                .filter(d -> d.getEliminadoEn() == null)
                .map(documentoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocumentoResponseDTO> listarPorCaso(Integer idCaso, org.springframework.data.domain.Pageable pageable) {
        currentUser.requireCaso(idCaso);
        return documentoRepository.findActivosByCasoId(idCaso, pageable).map(documentoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocumentoResponseDTO> listarPapelera(Integer firmaId, org.springframework.data.domain.Pageable pageable) {
        Integer firmaToken = currentUser.isAdmin() && firmaId != null ? firmaId : currentUser.requireFirmaId();
        return documentoRepository.findPapeleraByFirmaId(firmaToken, pageable).map(documentoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO buscarPorId(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        // P1-B7: un documento en papelera no se descarga ni se muestra como activo.
        if (documento.getEliminadoEn() != null) {
            throw new EntityNotFoundException("Documento no encontrado");
        }
        currentUser.requireCaso(documento.getCaso().getId());
        return documentoMapper.toResponse(documento);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer firmaDeCaso(Integer idCaso) {
        currentUser.requireCaso(idCaso);
        return casoRepository.findById(idCaso)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"))
                .getFirma().getId();
    }

    @Override
    public void eliminar(Integer id) {
        // P1-B7: DELETE ya NO borra físicamente. Envía a papelera: se
        // conservan fila y archivo, y queda auditado. El borrado físico
        // solo lo hace un admin con eliminarDefinitivo (abajo). Retención
        // documentada en README: 30 días en papelera, luego el admin puede
        // borrarlo definitivamente; no hay purga automática en P1.
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        currentUser.requireCaso(documento.getCaso().getId());
        if (documento.getEliminadoEn() != null) {
            return; // Ya está en papelera: idempotente.
        }
        documento.setEliminadoEn(LocalDateTime.now());
        documento.setEliminadoPor(currentUser.personaId());
        documentoRepository.save(documento);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "DOCUMENTO_PAPELERA", "documento", id, null);
    }

    @Override
    public void restaurar(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        currentUser.requireCaso(documento.getCaso().getId());
        documento.setEliminadoEn(null);
        documento.setEliminadoPor(null);
        documentoRepository.save(documento);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "DOCUMENTO_RESTAURADO", "documento", id, null);
    }

    @Override
    public void eliminarDefinitivo(Integer id) {
        // Solo admin: el borrado físico es irreversible y queda auditado
        // ANTES de ejecutarse, como en P0.
        if (!currentUser.isAdmin()) {
            throw new SecurityException("Solo un administrador puede borrar definitivamente un documento");
        }
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        currentUser.requireCaso(documento.getCaso().getId());
        String rutaArchivo = documento.getDireccion();
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "DOCUMENTO_ELIMINADO_DEFINITIVO", "documento", id, null);
        if (rutaArchivo != null && !rutaArchivo.isEmpty()) {
            fileStorageService.eliminarArchivo(rutaArchivo);
        }
        borrarRegistroBD(documento);
    }

    @Transactional
    public void borrarRegistroBD(Documento documento) {
        documentoRepository.delete(documento);
    }
}