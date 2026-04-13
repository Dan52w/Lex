package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;
import com.example.lexcloud.service.FirmaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/firmas")
public class FirmaController {

    private final FirmaService firmaService;

    public FirmaController(FirmaService firmaService) {
        this.firmaService = firmaService;
    }

    // 1. Registrar una nueva firma
    @PostMapping
    public ResponseEntity<FirmaResponseDTO> guardar(@Valid @RequestBody FirmaRequestDTO request) {
        return new ResponseEntity<>(firmaService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Buscar una firma por su NIT (Útil para login o validaciones)
    @GetMapping("/nit/{nit}")
    public ResponseEntity<FirmaResponseDTO> buscarPorNit(@PathVariable String nit) {
        return firmaService.buscarPorNit(nit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<FirmaResponseDTO> buscarPorId(@PathVariable Integer id) {
        return firmaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Listar firmas por nombre (Búsqueda de texto)
    @GetMapping("/buscar/{nombre}")
    public ResponseEntity<List<FirmaResponseDTO>> listarPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(firmaService.listarPorNombre(nombre));
    }

    // 5. Actualizar datos de la firma
    @PutMapping("/{id}")
    public ResponseEntity<FirmaResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody FirmaRequestDTO request) {
        return ResponseEntity.ok(firmaService.actualizar(id, request));
    }
}