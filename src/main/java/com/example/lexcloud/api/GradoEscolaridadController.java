package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.GradoEscolaridadRequestDTO;
import com.example.lexcloud.dto.response.GradoEscolaridadResponseDTO;
import com.example.lexcloud.service.GradoEscolaridadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/grado-escolaridades")
public class GradoEscolaridadController {
    private final GradoEscolaridadService gradoEscolaridadService;

    public GradoEscolaridadController(GradoEscolaridadService gradoEscolaridadService) {
        this.gradoEscolaridadService = gradoEscolaridadService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<GradoEscolaridadResponseDTO> guardar(@Valid @RequestBody GradoEscolaridadRequestDTO request) {
        return new ResponseEntity<>(gradoEscolaridadService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GradoEscolaridadResponseDTO>> listarTodo() {
        return ResponseEntity.ok(gradoEscolaridadService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradoEscolaridadResponseDTO> buscarPorId(@PathVariable Integer id) {
        return gradoEscolaridadService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<GradoEscolaridadResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return gradoEscolaridadService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<GradoEscolaridadResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(gradoEscolaridadService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<GradoEscolaridadResponseDTO> actualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody GradoEscolaridadRequestDTO request) {
        return ResponseEntity.ok(gradoEscolaridadService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        gradoEscolaridadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
