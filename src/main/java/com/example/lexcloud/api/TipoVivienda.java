package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoViviendaRequestDTO;
import com.example.lexcloud.dto.response.TipoViviendaResponseDTO;
import com.example.lexcloud.service.TipoViviendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tipo-viviendas")
public class TipoVivienda {
    private final TipoViviendaService tipoViviendaService;

    public TipoVivienda(TipoViviendaService tipoViviendaService) {
        this.tipoViviendaService = tipoViviendaService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipoViviendaResponseDTO> guardar(@Valid @RequestBody TipoViviendaRequestDTO request) {
        return new ResponseEntity<>(tipoViviendaService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoViviendaResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoViviendaService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoViviendaResponseDTO> buscarPorId(@PathVariable Integer id) {
        return tipoViviendaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoViviendaResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return tipoViviendaService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TipoViviendaResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(tipoViviendaService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoViviendaResponseDTO> actualizar(@PathVariable Integer id,
                                                                    @Valid @RequestBody TipoViviendaRequestDTO request) {
        return ResponseEntity.ok(tipoViviendaService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoViviendaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
