package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.security.FileValidation;
import com.example.lexcloud.service.DocumentoService;
import com.example.lexcloud.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {
    private final DocumentoService documentoService;
    private final FileStorageService fileStorageService;
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;

    /** Límite por documento. Por defecto 10 MB (P0-B5). */
    @Value("${lexcloud.documentos.max-bytes:10485760}")
    private long maxBytes;

    public DocumentoController(DocumentoService documentoService, FileStorageService fileStorageService,
                               CurrentUser currentUser, AuditoriaService auditoriaService) {
        this.documentoService = documentoService;
        this.fileStorageService = fileStorageService;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirDocumento(
            @RequestPart("datos") @Valid DocumentoRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) throws Exception {

        // P0-B5: validación en el backend (tamaño, lista blanca, magic bytes).
        // El nitFirma del DTO se ignora: la carpeta sale de la firma del caso
        // resuelta en el servidor. Nota: getBytes() carga el archivo en
        // memoria; con el límite de 10 MB es aceptable en P0 (antes no había
        // límite efectivo y el front permitía 200 MB).
        byte[] contenido = archivo.getBytes();
        String extension = FileValidation.validar(contenido, archivo.getOriginalFilename(), maxBytes);

        // Acceso al caso + firma del servidor (lanza 403 si no hay acceso).
        Integer firmaId = documentoService.firmaDeCaso(request.idCaso());
        String carpeta = "firma_" + firmaId;
        String nombreAlmacenamiento = FileValidation.nombreAlmacenamiento(extension);

        String rutaRelativa = fileStorageService.guardarArchivo(archivo, carpeta, nombreAlmacenamiento);

        DocumentoResponseDTO response = documentoService.guardar(request, rutaRelativa, contenido.length);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPodId(@PathVariable Integer id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<PageResponse<DocumentoResponseDTO>> listarPorCaso(
            @PathVariable Integer idCaso,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(documentoService.listarPorCaso(idCaso, Pagination.of(page, size))));
    }

    // P1-B7 papelera: documentos eliminados (no borrados físicamente).
    @GetMapping("/papelera")
    public ResponseEntity<PageResponse<DocumentoResponseDTO>> listarPapelera(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(documentoService.listarPapelera(currentUser.firmaId(), Pagination.of(page, size))));
    }

    @PutMapping("/{id}/restaurar")
    public ResponseEntity<Void> restaurar(@PathVariable Integer id) {
        documentoService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    // Solo admin (lo comprueba el servicio): borrado físico definitivo.
    @DeleteMapping("/{id}/definitivo")
    public ResponseEntity<Void> eliminarDefinitivo(@PathVariable Integer id) {
        documentoService.eliminarDefinitivo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        // buscarPorId ya comprueba acceso al caso (P0-B2).
        DocumentoResponseDTO dto = documentoService.buscarPorId(id);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "DOCUMENTO_DESCARGADO", "documento", id, null);
        Resource recurso = fileStorageService.cargarArchivo(dto.direccion());

        // La extensión real está en la ruta almacenada, no en la categoría.
        String extension = FileValidation.extensionDe(dto.direccion());

        // P0-B5: nombre sanitizado y escapado; attachment para que el
        // navegador no ejecute el contenido como HTML aunque sea un PDF con
        // script o un archivo renombrado. El Content-Type sale de la lista
        // blanca, nunca del Content-Type que envió el cliente.
        String nombreLimpio = FileValidation.sanitizarNombreParaMostrar(dto.nombre());
        String nombreConExtension = (!extension.isEmpty()
                && !nombreLimpio.toLowerCase().endsWith("." + extension))
                ? nombreLimpio + "." + extension
                : nombreLimpio;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileValidation.mediaTypePara(extension)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreConExtension + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
