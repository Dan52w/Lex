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
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentoServiceImpl implements DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoServiceImpl.class);

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
        // M22: validaciones de entrada del servicio (no se confía solo en el
        // controlador: la subida ya ocurrió cuando se llega aquí, así que
        // cualquier rechazo debe ser compensado por el llamador borrando el
        // archivo — ver DocumentoController).
        Validaciones.requerirNoNulo(request, "request");
        Validaciones.requerirId(request.idCaso(), "idCaso");
        Validaciones.requerirId(request.idTipoDocumento(), "idTipoDocumento");
        String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre");
        if (nombre.length() > 255) {
            throw new IllegalArgumentException("El nombre del documento es demasiado largo.");
        }
        if (direccionArchivo == null || direccionArchivo.isBlank()) {
            throw new IllegalArgumentException("La dirección del archivo es obligatoria.");
        }
        if (pesoEnBytes < 0 || pesoEnBytes > FileStorageServiceImpl.TAMANIO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El tamaño del archivo no es válido.");
        }

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
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));
        // M9: no se suben documentos a un caso inactivo.
        if (!Boolean.TRUE.equals(caso.getActivo())) {
            throw new IllegalArgumentException("No se puede subir un documento a un caso inactivo.");
        }
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
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado")));

        // 6. Persistencia.
        // A11: esta transacción cubre SOLO la BD. La subida al storage ya
        // ocurrió antes (en el controlador) y no es transaccional: si este
        // save falla, el controlador compensa borrando el archivo subido.
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
    @Transactional
    public void eliminar(Integer id) {
        // 1. Buscar el documento (Lectura rápida)
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));

        // 1b. Solo quien tiene acceso al caso puede borrar sus documentos.
        contextoSeguridad.verificarAccesoACaso(documento.getCaso().getId());

        String rutaArchivo = documento.getDireccion();

        // A11: se corrigió el diseño anterior (borrar el archivo físico
        // primero y el registro después mediante un método auxiliar con un
        // @Transactional ignorado por auto-llamada). Ahora el registro se
        // borra dentro de esta transacción de BD y el archivo después.
        // Límite transaccional: el storage no participa en la transacción;
        // si el borrado físico falla, el registro ya no existe y solo se
        // registra el fallo (quedaría un archivo huérfano en Supabase, que
        // es menos grave que un registro que apunta a un archivo inexistente).
        // El método auxiliar anterior se eliminó: era el que portaba el
        // comentario falso sobre la transacción.
        documentoRepository.delete(documento);

        if (rutaArchivo != null && !rutaArchivo.isEmpty()) {
            try {
                fileStorageService.eliminarArchivo(rutaArchivo);
            } catch (Exception e) {
                log.warn("No se pudo borrar el archivo físico {} tras eliminar el documento: {}",
                        rutaArchivo, e.getMessage());
            }
        }
    }
}
