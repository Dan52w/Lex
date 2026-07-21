package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.service.DocumentoService;
import com.example.lexcloud.service.FileStorageService;
import jakarta.validation.Valid;
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

    public DocumentoController(DocumentoService documentoService, FileStorageService fileStorageService) {
        this.documentoService = documentoService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirDocumento(
            @RequestPart("datos") @Valid DocumentoRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) {

        // 1. Validaciones preventivas
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("No se puede subir un archivo vacío.");
        }

        if (request.nitFirma() == null || request.nitFirma().isBlank()) {
            throw new IllegalArgumentException("El NIT de la firma es obligatorio para organizar el archivo.");
        }

        // 2. Metadatos del archivo
        String carpeta = "firma_" + request.nitFirma().trim();
        long pesoEnBytes = archivo.getSize(); // Capturamos el tamaño aquí

        // 3. Guardado físico (File System o Cloud)
        // El FileStorageService debería devolver la ruta donde quedó el archivo
        String rutaRelativa = fileStorageService.guardarArchivo(archivo, carpeta);

        // 4. Guardado en Base de Datos (Pasamos el peso obtenido)
        DocumentoResponseDTO response = documentoService.guardar(request, rutaRelativa, pesoEnBytes);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPodId(@PathVariable Integer id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<List<DocumentoResponseDTO>> listarPorCaso(@PathVariable Integer idCaso) {
        return ResponseEntity.ok(documentoService.listarPorCaso(idCaso));
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        DocumentoResponseDTO dto = documentoService.buscarPorId(id);
        Resource recurso = fileStorageService.cargarArchivo(dto.direccion());

        // La extensión real del archivo está en la ruta almacenada, no en la categoría (tipoDocumento)
        String extension = extraerExtension(dto.direccion());

        String nombreConExtension = (!extension.isEmpty() && !dto.nombre().toLowerCase().endsWith("." + extension))
                ? dto.nombre() + "." + extension
                : dto.nombre();

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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreConExtension + "\"")
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
