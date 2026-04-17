package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.TipoAbogadoResponseDTO;
import com.example.lexcloud.service.TipoAbogadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-abogados")
public class TipoAbogadoController {

    private final TipoAbogadoService tipoAbogadoService;

    public TipoAbogadoController(TipoAbogadoService tipoAbogadoService) {
        this.tipoAbogadoService = tipoAbogadoService;
    }

    @PostMapping
    public ResponseEntity<TipoAbogadoResponseDTO> crear(@Valid @RequestBody TipoAbogadoRequestDTO request) {
        TipoAbogadoResponseDTO nuevoTipo = tipoAbogadoService.guardar(request);
        return new ResponseEntity<>(nuevoTipo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoAbogadoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoAbogadoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoAbogadoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return tipoAbogadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoAbogadoResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return tipoAbogadoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoAbogadoResponseDTO> actualizar(@PathVariable Integer id,
                                                             @Valid @RequestBody TipoAbogadoRequestDTO request) {
        return ResponseEntity.ok(tipoAbogadoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoAbogadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}