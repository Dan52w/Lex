package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;
import com.example.lexcloud.service.SubtipoCasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/subtipo-casos")
public class SubtipoCasoController {

    private final SubtipoCasoService subtipoCasoService;

    public SubtipoCasoController(SubtipoCasoService subtipoCasoService) {
        this.subtipoCasoService = subtipoCasoService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SubtipoCasoResponseDTO> crear(@Valid @RequestBody SubtipoCasoRequestDTO request) {
        return new ResponseEntity<>(subtipoCasoService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SubtipoCasoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(subtipoCasoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubtipoCasoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return subtipoCasoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint específico para filtrar por el padre (Tipo de Caso)
    @GetMapping("/tipo/{idTipoCaso}")
    public ResponseEntity<List<SubtipoCasoResponseDTO>> listarPorTipo(@PathVariable Integer idTipoCaso) {
        List<SubtipoCasoResponseDTO> lista = subtipoCasoService.listarPorTipo(idTipoCaso);
        return ResponseEntity.ok(lista);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SubtipoCasoResponseDTO> actualizar(@PathVariable Integer id,
                                                             @Valid @RequestBody SubtipoCasoRequestDTO request) {
        return ResponseEntity.ok(subtipoCasoService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        subtipoCasoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}