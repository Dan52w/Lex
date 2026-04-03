package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;
import com.example.lexcloud.service.AbogadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/abogados")
public class AbogadoController {
    private final AbogadoService abogadoService;

    public AbogadoController(AbogadoService abogadoService) {
        this.abogadoService = abogadoService;
    }

    // 1. Crear Abogado
    @PostMapping
    public ResponseEntity<AbogadoResponseDTO> guardar(@Valid @RequestBody AbogadoRequestDTO request) {
        return new ResponseEntity<>(abogadoService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Listar todos por Firma (Activos)
    @GetMapping("/firma/{idFirma}")
    public ResponseEntity<List<AbogadoResponseDTO>> listarPorFirma(@PathVariable Integer idFirma) {
        return ResponseEntity.ok(abogadoService.listarTodos(idFirma));
    }

    // 3. Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<AbogadoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return abogadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{idFirma}/{especialidad}")
    public ResponseEntity<List<AbogadoResponseDTO>> listarEspecialidad(@PathVariable Integer idFirma, @PathVariable String especialidad) {
        return ResponseEntity.ok(abogadoService.listarTipo(idFirma, especialidad));
    }

    @GetMapping("nombre/{idFirma}/{nombre}")
    public ResponseEntity<List<AbogadoResponseDTO>> listarNombre(@PathVariable Integer idFirma, @PathVariable String nombre) {
        return ResponseEntity.ok(abogadoService.listarNombre(idFirma, nombre));
    }

    @GetMapping("/cedula/{idFirma}/{cc}")
    public ResponseEntity<AbogadoResponseDTO> buscarPorCc(@PathVariable Integer idFirma, @PathVariable String cc) {
        return abogadoService.buscarPorCc(cc, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<AbogadoResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody AbogadoRequestDTO request) {
        return ResponseEntity.ok(abogadoService.actualizar(id, request));
    }

    // 5. Desactivar (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        abogadoService.desactivarAbogado(id);
        return ResponseEntity.noContent().build();
    }
}
