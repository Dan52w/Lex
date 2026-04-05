package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.AgendaRequestDTO;
import com.example.lexcloud.dto.response.AgendaResponseDTO;
import com.example.lexcloud.service.AgentaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendas")
public class AgendaController {

    private final AgentaService agentaService;

    public AgendaController(AgentaService agentaService) {
        this.agentaService = agentaService;
    }

    @PostMapping
    public ResponseEntity<AgendaResponseDTO> crearAgenda(@Valid @RequestBody AgendaRequestDTO request) {
        return new ResponseEntity<>(agentaService.guardar(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendaResponseDTO> buscarPorId(@PathVariable Integer id) {
        return agentaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/abogado/{idAbogado}")
    public ResponseEntity<List<AgendaResponseDTO>> listarPorAbogado(@PathVariable Integer idAbogado) {
        return ResponseEntity.ok(agentaService.listarAbogado(idAbogado));
    }

    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<List<AgendaResponseDTO>> listarPorCaso(@PathVariable Integer idCaso) {
        return ResponseEntity.ok(agentaService.listarCaso(idCaso));
    }

    // Mejora: Uso de Query Params para fechas en lugar de Body
    // Ejemplo: /api/agendas/abogado/5/rango?inicio=2026-04-01T00:00:00&fin=2026-04-30T23:59:59
    @GetMapping("/abogado/{idAbogado}/rango")
    public ResponseEntity<List<AgendaResponseDTO>> listarPorAbogadoFechaRange(@PathVariable Integer idAbogado,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(agentaService.listarAbogadoFechaRange(idAbogado, inicio, fin));
    }

    @GetMapping("/caso/{idCaso}/rango")
    public ResponseEntity<List<AgendaResponseDTO>> listarPorCasoFechaRange(@PathVariable Integer idCaso,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(agentaService.listarCasoFechaRange(idCaso, inicio, fin));
    }

    @GetMapping("/abogado/citas/{idAbogado}")
    public ResponseEntity<List<AgendaResponseDTO>> listarPorProximasCitas(@PathVariable Integer idAbogado) {
        return ResponseEntity.ok(agentaService.listarProximaCitas(idAbogado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendaResponseDTO> actualizar(@PathVariable Integer id,@Valid @RequestBody AgendaRequestDTO request) {
        return ResponseEntity.ok(agentaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        agentaService.desactivarAgenda(id);
        return ResponseEntity.noContent().build();
    }
}