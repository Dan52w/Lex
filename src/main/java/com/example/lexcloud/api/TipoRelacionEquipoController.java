package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoRelacionEquipoRequestDTO;
import com.example.lexcloud.dto.response.TipoRelacionEquipoResponseDTO;
import com.example.lexcloud.service.TipoRelacionEquipoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tipo-relacion-equipos")
public class TipoRelacionEquipoController {
    private final TipoRelacionEquipoService tipoRelacionEquipoService;

    public TipoRelacionEquipoController(TipoRelacionEquipoService tipoRelacionEquipoService) {
        this.tipoRelacionEquipoService = tipoRelacionEquipoService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipoRelacionEquipoResponseDTO> guardar(@Valid @RequestBody TipoRelacionEquipoRequestDTO request) {
        return new ResponseEntity<>(tipoRelacionEquipoService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoRelacionEquipoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoRelacionEquipoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoRelacionEquipoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return tipoRelacionEquipoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoRelacionEquipoResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return tipoRelacionEquipoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TipoRelacionEquipoResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(tipoRelacionEquipoService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoRelacionEquipoResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody TipoRelacionEquipoRequestDTO request) {
        return ResponseEntity.ok(tipoRelacionEquipoService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoRelacionEquipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
