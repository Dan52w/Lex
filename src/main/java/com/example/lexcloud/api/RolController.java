package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.RolRequestDTO;
import com.example.lexcloud.dto.response.RolResponseDTO;
import com.example.lexcloud.service.RolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @PostMapping
    public ResponseEntity<RolResponseDTO> crear(@Valid @RequestBody RolRequestDTO request) {
        return new ResponseEntity<>(rolService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listarTodo() {
        return ResponseEntity.ok(rolService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<RolResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return rolService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<RolResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(rolService.listarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody RolRequestDTO request) {
        return ResponseEntity.ok(rolService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}