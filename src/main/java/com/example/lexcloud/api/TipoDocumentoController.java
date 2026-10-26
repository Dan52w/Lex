package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoDocumentoRequestDTO;
import com.example.lexcloud.dto.response.TipoDocumentoResponseDTO;
import com.example.lexcloud.service.TipoDocumentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tipo-documentos")
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;

    public TipoDocumentoController(TipoDocumentoService tipoDocumentoService) {
        this.tipoDocumentoService = tipoDocumentoService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipoDocumentoResponseDTO> crear(@Valid @RequestBody TipoDocumentoRequestDTO request) {
        TipoDocumentoResponseDTO nuevoTipo = tipoDocumentoService.guardar(request);
        return new ResponseEntity<>(nuevoTipo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoDocumentoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoDocumentoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoDocumentoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return tipoDocumentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoDocumentoResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return tipoDocumentoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoDocumentoResponseDTO> actualizar(@PathVariable Integer id,
                                                               @Valid @RequestBody TipoDocumentoRequestDTO request) {
        return ResponseEntity.ok(tipoDocumentoService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoDocumentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}