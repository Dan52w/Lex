package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.service.CasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casos")
public class CasoController {

    private final CasoService casoService;

    public CasoController(CasoService casoService) {
        this.casoService = casoService;
    }

    // 1. Crear un nuevo caso
    @PostMapping
    public ResponseEntity<CasoResponseDTO> crearCaso(@Valid @RequestBody CasoRequestDTO request) {
        return new ResponseEntity<>(casoService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Buscar un caso específico por ID
    @GetMapping("/{id}")
    public ResponseEntity<CasoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(casoService.buscarPorId(id));
    }

    // 3. Listar todos los casos vinculados y autorizados de una persona
    @GetMapping("/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorPersona(@PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarVinculadosPersonaAutorizado(idPersona));
    }

    // 4. Buscar por título (parcial) y persona
    // Ejemplo: /api/casos/buscar/titulo?idPersona=5&query=demanda
    @GetMapping("/buscar/titulo")
    public ResponseEntity<List<CasoResponseDTO>> buscarPorTitulo(
            @RequestParam Integer idPersona,
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.listarPorTitulo(query, idPersona));
    }

    // 5. Listar por nombre de estado y persona
    @GetMapping("/estado/{estadoNombre}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorEstado(
            @PathVariable String estadoNombre,
            @PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarPorEstado(estadoNombre, idPersona));
    }

    // 6. Listar por subtipo y persona
    @GetMapping("/subtipo/{idSubtipo}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorSubtipo(
            @PathVariable Integer idSubtipo,
            @PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarPorSubtipo(idSubtipo, idPersona));
    }

    // 7. Filtro avanzado: Subtipo + Estado + Persona
    @GetMapping("/filtro-avanzado")
    public ResponseEntity<List<CasoResponseDTO>> filtrarAvanzado(
            @RequestParam Integer idPersona,
            @RequestParam Integer idEstado,
            @RequestParam Integer idSubtipo) {
        return ResponseEntity.ok(casoService.listarPorSubtipoAndEstado(idPersona, idEstado, idSubtipo));
    }

    // 8. Actualizar caso
    @PutMapping("/{id}")
    public ResponseEntity<CasoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CasoRequestDTO request) {
        return ResponseEntity.ok(casoService.actualizar(id, request));
    }

    // 9. Desactivar caso (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        casoService.desactivarCaso(id);
        return ResponseEntity.noContent().build();
    }
}