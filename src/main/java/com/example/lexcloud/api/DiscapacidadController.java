package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.DiscapacidadRequestDTO;
import com.example.lexcloud.dto.response.DiscapacidadResponseDTO;
import com.example.lexcloud.service.DiscapacidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discapacidades")
public class DiscapacidadController {

    private final DiscapacidadService discapacidadService;

    public DiscapacidadController(DiscapacidadService discapacidadService) {
        this.discapacidadService = discapacidadService;
    }

    @PostMapping
    public ResponseEntity<DiscapacidadResponseDTO> guardar(@Valid @RequestBody DiscapacidadRequestDTO request) {
        return new ResponseEntity<>(discapacidadService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DiscapacidadResponseDTO>> listarTodo() {
        return ResponseEntity.ok(discapacidadService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscapacidadResponseDTO> buscarPorId(@PathVariable Integer id) {
        return discapacidadService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<DiscapacidadResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return discapacidadService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<DiscapacidadResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(discapacidadService.listarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscapacidadResponseDTO> actualizar(@PathVariable Integer id,
                                                              @Valid @RequestBody DiscapacidadRequestDTO request) {
        return ResponseEntity.ok(discapacidadService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        discapacidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}