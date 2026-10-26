package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.EstadoRequestDTO;
import com.example.lexcloud.dto.response.EstadoResponseDTO;
import com.example.lexcloud.service.EstadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/estados")
public class EstadoController {

    private final EstadoService estadoService;

    public EstadoController(EstadoService estadoService) {
        this.estadoService = estadoService;
    }

    // 1. Crear un nuevo estado
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EstadoResponseDTO> guardar(@Valid @RequestBody EstadoRequestDTO request) {
        return new ResponseEntity<>(estadoService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Listar todos los estados (Útil para llenar Selects/Dropdowns en el Front)
    @GetMapping
    public ResponseEntity<List<EstadoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estadoService.listarTodos());
    }

    // 3. Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<EstadoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return estadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Buscar por nombre exacto
    @GetMapping("/buscar/{nombre}")
    public ResponseEntity<EstadoResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return estadoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Actualizar un estado existente
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EstadoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoRequestDTO request) {
        return ResponseEntity.ok(estadoService.actualizar(id, request));
    }

    // 6. Eliminar un estado
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}