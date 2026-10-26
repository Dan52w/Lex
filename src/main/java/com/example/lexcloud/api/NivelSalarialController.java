package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.NivelSalarialRequestDTO;
import com.example.lexcloud.dto.response.NivelSalarialResponseDTO;
import com.example.lexcloud.service.NivelSalarialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/nivel-salariales")
public class NivelSalarialController {
    private final NivelSalarialService nivelSalarialService;

    public NivelSalarialController(NivelSalarialService nivelSalarialService) {
        this.nivelSalarialService = nivelSalarialService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<NivelSalarialResponseDTO> guardar(@Valid @RequestBody NivelSalarialRequestDTO request) {
        return new ResponseEntity<>(nivelSalarialService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NivelSalarialResponseDTO>> listarTodo() {
        return ResponseEntity.ok(nivelSalarialService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NivelSalarialResponseDTO> buscarPorId(@PathVariable Integer id) {
        return nivelSalarialService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rango/{rango}")
    public ResponseEntity<NivelSalarialResponseDTO> buscarPorRango(@PathVariable String rango) {
        return nivelSalarialService.buscarPorRango(rango)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<NivelSalarialResponseDTO>> listarPorRango(@RequestParam String nombre) {
        return ResponseEntity.ok(nivelSalarialService.listarPorRango(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<NivelSalarialResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody NivelSalarialRequestDTO request) {
        return ResponseEntity.ok(nivelSalarialService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        nivelSalarialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
