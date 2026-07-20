package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.GeneroRequestDTO;
import com.example.lexcloud.dto.response.GeneroResponseDTO;
import com.example.lexcloud.service.GeneroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generos")
public class GeneroController {
    private final GeneroService generoService;

    public GeneroController(GeneroService generoService) {
        this.generoService = generoService;
    }

    @PostMapping
    public ResponseEntity<GeneroResponseDTO> guardar(@Valid @RequestBody GeneroRequestDTO request) {
        return new ResponseEntity<>(generoService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GeneroResponseDTO>> listarTodo() {
        return ResponseEntity.ok(generoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneroResponseDTO> buscarPorId(@PathVariable Integer id) {
        return generoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<GeneroResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return generoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<GeneroResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(generoService.listarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneroResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody GeneroRequestDTO request) {
        return ResponseEntity.ok(generoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        generoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
