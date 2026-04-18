package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.VinculoRequestDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;
import com.example.lexcloud.service.VinculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vinculos")
public class VinculoController {

    private final VinculoService vinculoService;

    public VinculoController(VinculoService vinculoService) {
        this.vinculoService = vinculoService;
    }

    @PostMapping
    public ResponseEntity<VinculoResponseDTO> crear(@Valid @RequestBody VinculoRequestDTO request) {
        VinculoResponseDTO nuevoVinculo = vinculoService.guardar(request);
        return new ResponseEntity<>(nuevoVinculo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<VinculoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(vinculoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VinculoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return vinculoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<VinculoResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return vinculoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VinculoResponseDTO> actualizar(@PathVariable Integer id,
                                                         @Valid @RequestBody VinculoRequestDTO request) {
        return ResponseEntity.ok(vinculoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        vinculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}