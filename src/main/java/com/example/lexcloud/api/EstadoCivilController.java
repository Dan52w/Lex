package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.EstadoCivilRequestDTO;
import com.example.lexcloud.dto.response.EstadoCivilResponseDTO;
import com.example.lexcloud.service.EstadoCivilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/estados-civiles")
public class EstadoCivilController {

    private final EstadoCivilService estadoCivilService;

    public EstadoCivilController(EstadoCivilService estadoCivilService) {
        this.estadoCivilService = estadoCivilService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EstadoCivilResponseDTO> guardar(@Valid @RequestBody EstadoCivilRequestDTO request) {
        return new ResponseEntity<>(estadoCivilService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EstadoCivilResponseDTO>> listarTodo() {
        return ResponseEntity.ok(estadoCivilService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoCivilResponseDTO> buscarPorId(@PathVariable Integer id) {
        return estadoCivilService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<EstadoCivilResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return estadoCivilService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<EstadoCivilResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(estadoCivilService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EstadoCivilResponseDTO> actualizar(@PathVariable Integer id,
                                                             @Valid @RequestBody EstadoCivilRequestDTO request) {
        return ResponseEntity.ok(estadoCivilService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoCivilService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}