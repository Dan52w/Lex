package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.EstratoEconomicoRequestDTO;
import com.example.lexcloud.dto.response.EstratoEconomicoResponseDTO;
import com.example.lexcloud.service.EstratoEconomicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estrato-economicos")
public class EstratoEconomicoController {
    private final EstratoEconomicoService estratoEconomicoService;

    public EstratoEconomicoController(EstratoEconomicoService estratoEconomicoService) {
        this.estratoEconomicoService = estratoEconomicoService;
    }

    @PostMapping
    public ResponseEntity<EstratoEconomicoResponseDTO> guardar(@Valid @RequestBody EstratoEconomicoRequestDTO request) {
        return new ResponseEntity<>(estratoEconomicoService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EstratoEconomicoResponseDTO>> listarTodo() {
        return ResponseEntity.ok(estratoEconomicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstratoEconomicoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return estratoEconomicoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<EstratoEconomicoResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return estratoEconomicoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<EstratoEconomicoResponseDTO>> listarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(estratoEconomicoService.listarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstratoEconomicoResponseDTO> actualizar(@PathVariable Integer id,
                                                              @Valid @RequestBody EstratoEconomicoRequestDTO request) {
        return ResponseEntity.ok(estratoEconomicoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estratoEconomicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
