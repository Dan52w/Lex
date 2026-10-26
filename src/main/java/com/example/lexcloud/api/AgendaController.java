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

    // P1-B2: paginado (el front CasoAgendaTab ya usa apiPage aquí).
    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<PageResponse<AgendaResponseDTO>> listarPorCaso(
            @PathVariable Integer idCaso,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(agentaService.listarCaso(idCaso, Pagination.of(page, size))));
    }

    // Mejora: Uso de Query Params para fechas en lugar de Body
    // Ejemplo: /api/agendas/abogado/5/rango?inicio=2026-04-01T00:00:00&fin=2026-04-30T23:59:59
    // P1-B2: paginado (máx. 50/pág.).
    @GetMapping("/abogado/{idAbogado}/rango")
    public ResponseEntity<PageResponse<AgendaResponseDTO>> listarPorAbogadoFechaRange(@PathVariable Integer idAbogado,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(agentaService.listarAbogadoFechaRange(idAbogado, inicio, fin, Pagination.of(page, size))));
    }

    // P0-B2: ruta canónica sin ids del cliente (abogado del token).
    // P1-B2: paginado.
    @GetMapping("/mias/rango")
    public ResponseEntity<PageResponse<AgendaResponseDTO>> listarMisCitasRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(agentaService.listarMisCitasRango(inicio, fin, Pagination.of(page, size))));
    }

    // P1-B2: paginado.
    @GetMapping("/caso/{idCaso}/rango")
    public ResponseEntity<PageResponse<AgendaResponseDTO>> listarPorCasoFechaRange(@PathVariable Integer idCaso,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(agentaService.listarCasoFechaRange(idCaso, inicio, fin, Pagination.of(page, size))));
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