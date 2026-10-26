package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoIdentificacionRequestDTO;
import com.example.lexcloud.dto.response.TipoIdenficacionResponseDTO;
import com.example.lexcloud.service.TipoIdentificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tipo-identificaciones")
public class TipoIdentificacionController {
    private final TipoIdentificacionService tipoIdentificacionService;

    public TipoIdentificacionController(TipoIdentificacionService tipoIdentificacionService) {
        this.tipoIdentificacionService = tipoIdentificacionService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipoIdenficacionResponseDTO> guardar(@Valid @RequestBody TipoIdentificacionRequestDTO request) {
        return new ResponseEntity<>(tipoIdentificacionService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoIdenficacionResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoIdentificacionService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoIdenficacionResponseDTO> buscarPorId(@PathVariable Integer id) {
        return tipoIdentificacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoIdenficacionResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return tipoIdentificacionService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TipoIdenficacionResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(tipoIdentificacionService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoIdenficacionResponseDTO> actualizar(@PathVariable Integer id,
                                                              @Valid @RequestBody TipoIdentificacionRequestDTO request) {
        return ResponseEntity.ok(tipoIdentificacionService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoIdentificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
