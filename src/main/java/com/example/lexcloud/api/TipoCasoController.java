package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;
import com.example.lexcloud.service.TipoCasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-casos")
public class TipoCasoController {

    private final TipoCasoService tipoCasoService;

    public TipoCasoController(TipoCasoService tipoCasoService) {
        this.tipoCasoService = tipoCasoService;
    }

    @PostMapping
    public ResponseEntity<TipoCasoResponseDTO> crear(@Valid @RequestBody TipoCasoRequestDTO request) {
        TipoCasoResponseDTO nuevoTipo = tipoCasoService.guardar(request);
        return new ResponseEntity<>(nuevoTipo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoCasoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoCasoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCasoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return tipoCasoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoCasoResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return tipoCasoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoCasoResponseDTO> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody TipoCasoRequestDTO request) {
        return ResponseEntity.ok(tipoCasoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoCasoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}