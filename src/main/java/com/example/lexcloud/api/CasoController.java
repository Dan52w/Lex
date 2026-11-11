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

    // Crear un nuevo caso
    @PostMapping
    public ResponseEntity<CasoResponseDTO> crearCaso(@Valid @RequestBody CasoRequestDTO request) {
        return new ResponseEntity<>(casoService.guardar(request), HttpStatus.CREATED);
    }

    // Buscar un caso específico por ID
    @GetMapping("/{id}")
    public ResponseEntity<CasoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(casoService.buscarPorId(id));
    }

    // Actualizar caso
    @PutMapping("/{id}")
    public ResponseEntity<CasoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CasoRequestDTO request) {
        return ResponseEntity.ok(casoService.actualizar(id, request));
    }

    // Desactivar caso (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        casoService.desactivarCaso(id);
        return ResponseEntity.noContent().build();
    }

    // Reactivar caso (solo admin, guarda en el servicio).
    // El desactivar es DELETE /{id}; no hay PATCH /desactivar que espejear,
    // así que se usa PATCH /{id}/reactivar, verbo de cambio de estado que ya
    // usa VinculoCasoController (PATCH /{id}/autorizar).
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        casoService.reactivarCaso(id);
        return ResponseEntity.noContent().build();
    }

    // P0-B2: mis casos (canónico del front): la persona es la del token,
    // sin idPersona en la URL. Delega en listarPorPersona.
    @GetMapping("/mis-casos")
    public ResponseEntity<List<CasoResponseDTO>> listarMisCasos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarMisCasos(page, size));
    }

    // P0-B2: casos de un miembro de la firma. Solo admin o el propio
    // abogado (la guarda vive en el servicio).
    @GetMapping("/miembro/{idAbogado}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorMiembro(@PathVariable Integer idAbogado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorMiembro(idAbogado, page, size));
    }

    // Listar todos los casos vinculados y autorizados de una persona
    @GetMapping("/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorPersona(@PathVariable Integer idPersona,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarVinculadosPersonaAutorizado(idPersona, page, size));
    }

    // Listar casos autorizados por Abogado y Cliente (Uso interno)
    @GetMapping("/cliente/{idPersona}/{idCliente}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorCliente(
            @PathVariable Integer idPersona,
            @PathVariable Integer idCliente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarCasosAutorizadosPorAbogadoYCliente(idPersona, idCliente, page, size));
    }

    // Listar por caso padre
    @GetMapping("/padre/{idCaso}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorCasoPadre(@PathVariable Integer idCaso,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size){
        return ResponseEntity.ok(casoService.listarPorCasoPadre(idCaso, page, size));
    }

    // Buscar por título (parcial) y persona
    @GetMapping("/buscar/titulo")
    public ResponseEntity<List<CasoResponseDTO>> buscarPorTitulo(
            @RequestParam Integer idPersona,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorTitulo(query, idPersona, page, size));
    }

    // Listar por nombre de estado y persona
    @GetMapping("/estado/{estadoNombre}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorEstado(
            @PathVariable String estadoNombre,
            @PathVariable Integer idPersona,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorEstado(estadoNombre, idPersona, page, size));
    }

    // Listar por subtipo y persona
    @GetMapping("/subtipo/{idSubtipo}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorSubtipo(
            @PathVariable Integer idSubtipo,
            @PathVariable Integer idPersona,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorSubtipo(idSubtipo, idPersona, page, size));
    }

    // Filtro avanzado: Subtipo + Estado + Persona
    @GetMapping("/filtro-avanzado")
    public ResponseEntity<List<CasoResponseDTO>> filtrarAvanzado(
            @RequestParam Integer idPersona,
            @RequestParam Integer idEstado,
            @RequestParam Integer idSubtipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorSubtipoAndEstado(idSubtipo, idEstado, idPersona, page, size));
    }

    // Buscar por Radicado Interno de forma segura
    // Ejemplo: /api/casos/buscar/radicado-interno?idPersona=2&query=EXP-2026-A
    @GetMapping("/buscar/radicado-interno")
    public ResponseEntity<CasoResponseDTO> buscarPorRadicadoInterno(
            @RequestParam Integer idPersona,
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.buscarPorRadicadoInterno(query, idPersona));
    }

    // Buscar por Radicado Judicial de forma segura
    // Ejemplo: /api/casos/buscar/radicado-judicial?idPersona=2&query=11001928373
    @GetMapping("/buscar/radicado-judicial")
    public ResponseEntity<CasoResponseDTO> buscarPorRadicadoJudicial(
            @RequestParam Integer idPersona,
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.buscarPorRadicadoJudicial(query, idPersona));
    }

    // Súper Buscador (Título, Radicado Interno o Judicial bajo un solo término)
    // Ejemplo: /api/casos/buscar/global?idPersona=2&query=demanda
    @GetMapping("/buscar/global")
    public ResponseEntity<List<CasoResponseDTO>> superBuscador(
            @RequestParam Integer idPersona,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.superBuscador(query, idPersona, page, size));
    }

    // Listar por ID de Cliente exacto (Nueva implementación de apoyo)
    // Ejemplo: /api/casos/cliente-exacto/5/persona/2
    @GetMapping("/cliente-exacto/{idCliente}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorClienteExacto(
            @PathVariable Integer idCliente,
            @PathVariable Integer idPersona,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.listarPorCliente(idCliente, idPersona, page, size));
    }

    // Buscar casos por coincidencias en el Nombre o Identificación del Cliente (Texto libre)
    // Ejemplo: /api/casos/buscar/cliente?idPersona=2&query=10827
    @GetMapping("/buscar/cliente")
    public ResponseEntity<List<CasoResponseDTO>> buscarPorClienteNombreOIdentificacion(
            @RequestParam Integer idPersona,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(casoService.buscarPorNombreOIdentificacionCliente(query, idPersona, page, size));
    }
}