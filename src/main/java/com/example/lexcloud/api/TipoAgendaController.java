package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.TipoAgentaResquestDTO;
import com.example.lexcloud.dto.response.TipoAgendaResponseDTO;
import com.example.lexcloud.service.TipoAgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-agendas")
public class TipoAgendaController {

    private final TipoAgendaService tipoAgendaService;

    public TipoAgendaController(TipoAgendaService tipoAgendaService) {
        this.tipoAgendaService = tipoAgendaService;
    }

    @PostMapping
    public ResponseEntity<TipoAgendaResponseDTO> crear(@Valid @RequestBody TipoAgentaResquestDTO request) {
        TipoAgendaResponseDTO nuevoTipo = tipoAgendaService.guardar(request);
        return new ResponseEntity<>(nuevoTipo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TipoAgendaResponseDTO>> listarTodo() {
        return ResponseEntity.ok(tipoAgendaService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoAgendaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return tipoAgendaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<TipoAgendaResponseDTO> obtenerPorNombre(@PathVariable String nombre) {
        return tipoAgendaService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoAgendaResponseDTO> actualizar(@PathVariable Integer id,
                                                            @Valid @RequestBody TipoAgentaResquestDTO request) {
        return ResponseEntity.ok(tipoAgendaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoAgendaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}