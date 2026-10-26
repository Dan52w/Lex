package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.FirmaService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/firmas")
public class FirmaController {

    private final FirmaService firmaService;
    private final CurrentUser currentUser;

    public FirmaController(FirmaService firmaService, CurrentUser currentUser) {
        this.firmaService = firmaService;
        this.currentUser = currentUser;
    }

    // 1. Registrar una nueva firma (P0-B3: solo admin).
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FirmaResponseDTO> guardar(@Valid @RequestBody FirmaRequestDTO request) {
        return new ResponseEntity<>(firmaService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Buscar una firma por su NIT (P0-B2: solo admin; el NIT no debe
    // servir para enumerar firmas desde otra firma).
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nit/{nit}")
    public ResponseEntity<FirmaResponseDTO> buscarPorNit(@PathVariable String nit) {
        return firmaService.buscarPorNit(nit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Buscar por ID (P0-B2: admin o la propia firma del token).
    @GetMapping("/{id}")
    public ResponseEntity<FirmaResponseDTO> buscarPorId(@PathVariable Integer id) {
        currentUser.requireFirma(id);
        return firmaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Listar firmas por nombre (P0-B2: solo admin).
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/buscar/{nombre}")
    public ResponseEntity<List<FirmaResponseDTO>> listarPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(firmaService.listarPorNombre(nombre));
    }

    // 5. Actualizar datos de la firma (P0-B3: solo admin).
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FirmaResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody FirmaRequestDTO request) {
        return ResponseEntity.ok(firmaService.actualizar(id, request));
    }
}