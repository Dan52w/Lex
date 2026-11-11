package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;
import com.example.lexcloud.service.EquipoAbogadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipo-abogados")
public class EquipoAbogadoController {

    private final EquipoAbogadoService equipoAbogadoService;

    public EquipoAbogadoController(EquipoAbogadoService equipoAbogadoService) {
        this.equipoAbogadoService = equipoAbogadoService;
    }

    // 1. Crear una nueva asignación (Abogado <-> Persona con un Tipo de Relación)
    @PostMapping
    public ResponseEntity<EquipoAbogadoResponseDTO> crear(@Valid @RequestBody EquipoAbogadoRequestDTO request) {
        return new ResponseEntity<>(equipoAbogadoService.crearAsignacion(request), HttpStatus.CREATED);
    }

    // 2. Buscar una asignación puntual por su ID
    @GetMapping("/{id}")
    public ResponseEntity<EquipoAbogadoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(equipoAbogadoService.obtenerPorId(id));
    }

    // 3. Listar todas las asignaciones activas del sistema
    @GetMapping("/activos")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarTodosActivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarTodosActivos(page, size));
    }

    // 4. Todas las asignaciones (activas e inactivas) de un abogado
    @GetMapping("/abogado/{idAbogado}")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarPorAbogado(@PathVariable Integer idAbogado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarPorAbogado(idAbogado, page, size));
    }

    // 5. Solo las asignaciones activas de un abogado
    @GetMapping("/abogado/{idAbogado}/activos")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarActivosPorAbogado(@PathVariable Integer idAbogado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarActivosPorAbogado(idAbogado, page, size));
    }

    // 6. Asignaciones donde una Persona (staff) figura como asignador/asignado
    @GetMapping("/persona/{idPersona}")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarPorPersona(@PathVariable Integer idPersona,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarPorPersona(idPersona, page, size));
    }

    // 7. Asignaciones por tipo de relación (ej: "Líder", "Asistente")
    @GetMapping("/tipo-relacion/{idTipoRelacion}")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarPorTipoRelacion(@PathVariable Integer idTipoRelacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarPorTipoRelacion(idTipoRelacion, page, size));
    }

    // 8. Solo activas por tipo de relación
    @GetMapping("/tipo-relacion/{idTipoRelacion}/activos")
    public ResponseEntity<List<EquipoAbogadoResponseDTO>> listarActivosPorTipoRelacion(@PathVariable Integer idTipoRelacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(equipoAbogadoService.listarActivosPorTipoRelacion(idTipoRelacion, page, size));
    }

    // 9. Desactivar (soft) una asignación específica por par Abogado/Persona
    @PatchMapping("/desactivar")
    public ResponseEntity<Void> desactivar(@RequestParam Integer idAbogado, @RequestParam Integer idPersona) {
        equipoAbogadoService.desactivarAsignacion(idAbogado, idPersona);
        return ResponseEntity.noContent().build();
    }

    // 10. Eliminar físicamente el registro de asignación
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        equipoAbogadoService.eliminarAsignacion(id);
        return ResponseEntity.noContent().build();
    }
}