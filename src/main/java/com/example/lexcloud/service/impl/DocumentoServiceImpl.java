package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.DocumentoMapper;
import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Documento;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Documento_Repository;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.DocumentoService;
import com.example.lexcloud.service.FileStorageService;
import com.example.lexcloud.util.Paginacion;
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
    private final FileStorageService fileStorageService;
    private final ContextoSeguridad contextoSeguridad;

    public DocumentoServiceImpl(Documento_Repository documentoRepository,
                                Caso_Repository casoRepository,
                                TipoDocumento_Repository tipoDocumentoRepository,
                                DocumentoMapper documentoMapper,
                                FileStorageService fileStorageService,
                                ContextoSeguridad contextoSeguridad) {
        this.documentoRepository = documentoRepository;
        this.casoRepository = casoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.documentoMapper = documentoMapper;
        this.fileStorageService  = fileStorageService;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public DocumentoResponseDTO guardar(DocumentoRequestDTO request, String direccionArchivo, long pesoEnBytes) {
        // SVC-4: solo se suben documentos a casos a los que se tiene acceso.
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        // 1. Mapeo inicial de los datos básicos del DTO a la Entidad
        Documento documento = documentoMapper.toEntity(request);

        // W6: descripcion es NOT NULL en la entidad; sin valor se guarda vacía
        // (mismo criterio que AgendaServiceImpl con B3), en vez de un 500.
        if (documento.getDescripcion() == null) {
            documento.setDescripcion("");
        }

        // 2. Establecer metadatos técnicos del archivo
        documento.setDireccion(direccionArchivo);
        documento.setTamanio(pesoEnBytes); // El campo que definimos como BIGINT

        // 3. Vincular con el Caso
        // Usamos findById para asegurar que el caso existe antes de intentar guardar
        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("No se puede subir el documento: Caso no encontrado con ID: " + request.idCaso()));
        documento.setCaso(caso);

        // 3b. La carpeta la decide el caso, no el cliente: el NIT que viene en
        // el DTO debe ser el de la firma del caso; si no, el archivo quedaría
        // guardado en la carpeta de otra firma.
        if (request.nitFirma() == null || caso.getFirma() == null
                || !request.nitFirma().trim().equals(caso.getFirma().getNit())) {
            throw new SecurityException("El NIT de la firma no corresponde a la firma del caso.");
        }

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
        return listarPorCaso(idCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarPorCaso(Integer idCaso, int page, int size) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        return documentoRepository.findByCasoId(idCaso, Paginacion.crear(page, size)).stream()
                .map(documentoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO buscarPorId(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        contextoSeguridad.verificarAccesoACaso(documento.getCaso().getId());
        return documentoMapper.toResponse(documento);
    }

    @Override
    public void eliminar(Integer id) {
        // 1. Buscar el documento (Lectura rápida)
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));

        // 1b. Solo quien tiene acceso al caso puede borrar sus documentos.
        contextoSeguridad.verificarAccesoACaso(documento.getCaso().getId());

        String rutaArchivo = documento.getDireccion();

        // 2. Borrar de Supabase FUERA de una transacción pesada de escritura de BD
        if (rutaArchivo != null && !rutaArchivo.isEmpty()) {
            fileStorageService.eliminarArchivo(rutaArchivo);
        }

        // 3. Borrar de la base de datos (Esto sí es una operación transaccional pura y veloz)
        // Nota: si el borrado físico falla, la excepción revierte la transacción y
        // el registro queda; si el borrado físico funciona y el de BD falla, el
        // archivo ya no existe pero el registro queda huérfano (se detecta por
        // la descarga, que fallará con 500 genérico). No hay compensación aquí.
        borrarRegistroBD(documento);
    }

    @Transactional
    public void borrarRegistroBD(Documento documento) {
        documentoRepository.delete(documento);
    }
}
