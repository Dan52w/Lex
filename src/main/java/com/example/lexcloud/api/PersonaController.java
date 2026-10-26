package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PersonaResponseDTO> crear(@Valid @RequestBody PersonaRequestDTO request) {
        return new ResponseEntity<>(personaService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return personaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cc/{cc}")
    public ResponseEntity<PersonaResponseDTO> obtenerPorCc(@PathVariable String cc) {
        return personaService.buscarPorCc(cc)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activo/{id}")
    public ResponseEntity<PersonaResponseDTO> obtenerActivo(@PathVariable Integer id) {
        return personaService.buscarPorIdActivo(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // P0-B7: existe-cc solo-admin (antes público y enumeraba cédulas).
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/existe-cc/{cc}")
    public ResponseEntity<Boolean> existeCc(@PathVariable String cc) {
        return ResponseEntity.ok(personaService.existeCc(cc));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody PersonaRequestDTO request) {
        return ResponseEntity.ok(personaService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        personaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // P0-B3: reactivación EXPLÍCITA y solo-admin. Editar (PUT) ya no reactiva.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        personaService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}