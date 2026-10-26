package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoClienteRequestDTO;
import com.example.lexcloud.dto.response.TipoClienteResponseDTO;
import com.example.lexcloud.service.TipoClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tipo-clientes")
public class TipoClienteController {

    private final TipoClienteService tipoClienteService;

    public TipoClienteController(TipoClienteService tipoClienteService) {
        this.tipoClienteService = tipoClienteService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TipoClienteResponseDTO> crear(@Valid @RequestBody TipoClienteRequestDTO request) {
        return new ResponseEntity<>(tipoClienteService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoClienteResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoClienteService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoClienteResponseDTO> buscarPorId(@PathVariable Integer id) {
        return tipoClienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar/{nombre}")
    public ResponseEntity<List<TipoClienteResponseDTO>> buscarPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(tipoClienteService.listarPorNombre(nombre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoClienteResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody TipoClienteRequestDTO request) {
        return ResponseEntity.ok(tipoClienteService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoClienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}