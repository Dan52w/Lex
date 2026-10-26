package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
import com.example.lexcloud.service.HistorialCambioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial-cambios")
public class HistorialCambioController {

    private final HistorialCambioService historialService;

    public HistorialCambioController(HistorialCambioService historialService) {
        this.historialService = historialService;
    }

    // 1. Registrar un hito o cambio manualmente
    // Útil para cuando el abogado quiere agregar una nota de progreso
    @PostMapping
    public ResponseEntity<HistorialCambioResponseDTO> guardar(@Valid @RequestBody HistorialCambioRequestDTO request) {
        return new ResponseEntity<>(historialService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Obtener toda la línea de tiempo de un caso específico
    // GET /api/historial-cambios/caso/5
    @GetMapping("/caso/{idCaso}")
    public ResponseEntity<PageResponse<HistorialCambioResponseDTO>> listarPorCaso(
            @PathVariable Integer idCaso,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.of(historialService.listarPorCaso(idCaso, Pagination.of(page, size))));
    }
}