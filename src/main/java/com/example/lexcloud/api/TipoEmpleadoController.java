package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;
import com.example.lexcloud.service.TipoEmpleadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-empleados")
public class TipoEmpleadoController {
    private final TipoEmpleadoService tipoEmpleadoService;

    public TipoEmpleadoController(TipoEmpleadoService tipoEmpleadoService) {
        this.tipoEmpleadoService = tipoEmpleadoService;
    }

    @PostMapping
    public ResponseEntity<TipoEmpleadoResponseDTO> guardar(@Valid @RequestBody TipoEmpleadoRequestDTO request) {
        return new ResponseEntity<>(tipoEmpleadoService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoEmpleadoResponseDTO>> listarTodo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(tipoEmpleadoService.listarTodos(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoEmpleadoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return tipoEmpleadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoEmpleadoResponseDTO> buscarPorNombre(@PathVariable String nombre) {
        return tipoEmpleadoService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TipoEmpleadoResponseDTO>> listarPorNombre(@RequestParam String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(tipoEmpleadoService.listarPorNombre(nombre, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoEmpleadoResponseDTO> actualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody TipoEmpleadoRequestDTO request) {
        return ResponseEntity.ok(tipoEmpleadoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoEmpleadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
