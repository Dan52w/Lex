package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.service.VinculoCasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vinculo-casos")
public class VinculoCasoController {

    private final VinculoCasoService vinculoCasoService;

    public VinculoCasoController(VinculoCasoService vinculoCasoService) {
        this.vinculoCasoService = vinculoCasoService;
    }

    @PostMapping
    public ResponseEntity<VinculoCasoResponseDTO> vincular(@Valid @RequestBody VinculoCasoRequestDTO request) {
        return new ResponseEntity<>(vinculoCasoService.vincular(request), HttpStatus.CREATED);
    }

    @GetMapping("/caso/{idCaso}/personal")
    public ResponseEntity<List<VinculoCasoResponseDTO>> listarPersonalAsignado(@PathVariable Integer idCaso) {
        return ResponseEntity.ok(vinculoCasoService.listarPersonalAsignado(idCaso));
    }

    @GetMapping("/persona/{idPersona}/mis-casos")
    public ResponseEntity<List<VinculoCasoResponseDTO>> listarMisCasos(@PathVariable Integer idPersona) {
        return ResponseEntity.ok(vinculoCasoService.listarMisCasos(idPersona));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<VinculoCasoResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(vinculoCasoService.listarPendientesAutorizacion());
    }

    @GetMapping("/caso/{idCaso}/principal")
    public ResponseEntity<VinculoCasoResponseDTO> obtenerAbogadoPrincipal(@PathVariable Integer idCaso) {
        return vinculoCasoService.obtenerAbogadoPrincipal(idCaso)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Ajuste semántico REST: refleja que buscas el cliente "asociado a un caso" usando el idCaso
    @GetMapping("/caso/{idCaso}/cliente")
    public ResponseEntity<VinculoCasoResponseDTO> obtenerCliente(@PathVariable Integer idCaso) {
        return vinculoCasoService.obtenerCliente(idCaso)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/autorizar")
    public ResponseEntity<Void> autorizarAcceso(@PathVariable Integer id) {
        vinculoCasoService.autorizarAcceso(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVinculo(@PathVariable Integer id) {
        vinculoCasoService.eliminarVinculo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verificar-acceso")
    public ResponseEntity<Boolean> verificarAcceso(@RequestParam Integer idPersona, @RequestParam Integer idCaso) {
        return ResponseEntity.ok(vinculoCasoService.tieneAcceso(idPersona, idCaso));
    }
}