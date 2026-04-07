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

        // Validación preventiva
        if (request.nitFirma() == null || request.nitFirma().isBlank()) {
            throw new IllegalArgumentException("El NIT de la firma es obligatorio para organizar el archivo.");
        }

        // Estructura limpia: firma_123456789
        String carpeta = "firma_" + request.nitFirma().trim();

        // Guardamos físicamente
        String rutaRelativa = fileStorageService.guardarArchivo(archivo, carpeta);

        // Guardamos en Base de Datos
        return new ResponseEntity<>(documentoService.guardar(request, rutaRelativa), HttpStatus.CREATED);
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
