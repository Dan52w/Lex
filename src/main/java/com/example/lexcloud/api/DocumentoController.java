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
        // 1. Obtenemos los datos (donde tipoDocumento ahora es "pdf", "jpg", etc.)
        DocumentoResponseDTO dto = documentoService.buscarPorId(id);

        // 2. Cargamos el recurso físico
        Resource recurso = fileStorageService.cargarArchivo(dto.direccion());

        // 3. Construimos el nombre del archivo asegurando que tenga la extensión
        // Si el nombre ya tiene punto, lo dejamos; si no, se lo ponemos.
        String nombreConExtension = dto.nombre().toLowerCase().endsWith("." + dto.tipoDocumento().toLowerCase())
                ? dto.nombre()
                : dto.nombre() + "." + dto.tipoDocumento();

        // 4. Mapeo básico de Content-Type según tu extensión
        String mediaType = switch (dto.tipoDocumento().toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreConExtension + "\"")
                .body(recurso);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
