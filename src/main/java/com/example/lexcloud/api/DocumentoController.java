package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.DocumentoService;
import com.example.lexcloud.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {
    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);

    private final DocumentoService documentoService;
    private final FileStorageService fileStorageService;
    private final ContextoSeguridad contextoSeguridad;
    private final Caso_Repository casoRepository;
    private final TipoDocumento_Repository tipoDocumentoRepository;

    public DocumentoController(DocumentoService documentoService,
                               FileStorageService fileStorageService,
                               ContextoSeguridad contextoSeguridad,
                               Caso_Repository casoRepository,
                               TipoDocumento_Repository tipoDocumentoRepository) {
        this.documentoService = documentoService;
        this.fileStorageService = fileStorageService;
        this.contextoSeguridad = contextoSeguridad;
        this.casoRepository = casoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirDocumento(
            @RequestPart("datos") @Valid DocumentoRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) {

        // C9: validar PRIMERO, subir DESPUÉS. Nada se sube a Supabase hasta
        // que el acceso, el caso, el NIT, el tipo de documento y el propio
        // archivo han pasado todas las validaciones.
        if (request.nitFirma() == null || request.nitFirma().isBlank()) {
            throw new IllegalArgumentException("El NIT de la firma es obligatorio para organizar el archivo.");
        }
        validarAntesDeSubir(request, archivo);

        // Metadatos del archivo
        String carpeta = "firma_" + request.nitFirma().trim();
        long pesoEnBytes = archivo.getSize(); // Capturamos el tamaño aquí

        // Guardado físico (File System o Cloud)
        // El FileStorageService debería devolver la ruta donde quedó el archivo
        String rutaRelativa = fileStorageService.guardarArchivo(archivo, carpeta);

        // Guardado en Base de Datos (Pasamos el peso obtenido).
        // A11: la subida al storage NO está dentro de la transacción de BD:
        // si guardar() falla, el archivo ya subido se compensa borrándolo
        // (si el borrado también falla, solo se registra: no hay forma de
        // deshacer una subida con una transacción).
        try {
            DocumentoResponseDTO response = documentoService.guardar(request, rutaRelativa, pesoEnBytes);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            try {
                fileStorageService.eliminarArchivo(rutaRelativa);
            } catch (Exception ex) {
                log.warn("No se pudo borrar el archivo huérfano {} tras fallar el guardado del documento: {}",
                        rutaRelativa, ex.getMessage());
            }
            throw e;
        }
    }

    /**
     * C9: validaciones previas a cualquier subida. El servicio vuelve a
     * validar todo en {@code guardar()} (defensa en profundidad); esta
     * comprobación existe para que ningún archivo llegue al storage cuando
     * la petición va a ser rechazada.
     *
     * <p>Nota: la interfaz de DocumentoService está congelada en este lote,
     * así que la pre-validación vive aquí, con los repositorios que el
     * servicio ya usa; la carga del caso se repite en el servicio.</p>
     */
    private void validarAntesDeSubir(DocumentoRequestDTO request, MultipartFile archivo) {
        // Extensión real, Content-Type y tamaño (máx 10 MB) del archivo.
        // N10: por el bean inyectado (interfaz), no por la clase concreta.
        fileStorageService.validarArchivo(archivo);

        // Acceso al caso (404 genérico si no existe o no hay acceso).
        contextoSeguridad.verificarAccesoACaso(request.idCaso());

        Caso caso = casoRepository.findById(request.idCaso())
                .orElseThrow(() -> new EntityNotFoundException("Recurso no encontrado"));

        // M9: no se suben documentos a un caso inactivo.
        if (!Boolean.TRUE.equals(caso.getActivo())) {
            throw new IllegalArgumentException("No se puede subir un documento a un caso inactivo.");
        }

        // La carpeta la decide el caso, no el cliente: el NIT que viene en
        // el DTO debe ser el de la firma del caso.
        if (caso.getFirma() == null
                || !request.nitFirma().trim().equals(caso.getFirma().getNit())) {
            throw new SecurityException("El NIT de la firma no corresponde a la firma del caso.");
        }

        // Tipo de documento válido.
        if (request.idTipoDocumento() == null
                || !tipoDocumentoRepository.existsById(request.idTipoDocumento())) {
            throw new EntityNotFoundException("Recurso no encontrado");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<List<DocumentoResponseDTO>> listarPorCaso(@PathVariable Integer idCaso,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(documentoService.listarPorCaso(idCaso, page, size));
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        DocumentoResponseDTO dto = documentoService.buscarPorId(id);
        Resource recurso = fileStorageService.cargarArchivo(dto.direccion());

        // La extensión real del archivo está en la ruta almacenada, no en la categoría (tipoDocumento)
        String extension = extraerExtension(dto.direccion());

        String base = (dto.nombre() == null || dto.nombre().isBlank()) ? "documento" : dto.nombre();
        String nombreConExtension = (!extension.isEmpty() && !base.toLowerCase().endsWith("." + extension))
                ? base + "." + extension
                : base;

        String mediaType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                // M10: nunca concatenar el nombre a mano en el header: el
                // builder codifica (RFC 5987) y escapa lo necesario, así que
                // un nombre con comillas o saltos de línea no puede inyectar
                // cabeceras.
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreConExtension, StandardCharsets.UTF_8)
                                .toString())
                .body(recurso);
    }

    private String extraerExtension(String direccion) {
        if (direccion == null) return "";
        int lastDot = direccion.lastIndexOf('.');
        if (lastDot < 0 || lastDot == direccion.length() - 1) return "";
        return direccion.substring(lastDot + 1).toLowerCase();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
