package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.security.CurrentUser;
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
    private final CurrentUser currentUser;

    public CasoController(CasoService casoService, CurrentUser currentUser) {
        this.casoService = casoService;
        this.currentUser = currentUser;
    }

    // P0-B2: ruta canónica sin ids del cliente (persona del token).
    @GetMapping("/mis-casos")
    public ResponseEntity<List<CasoResponseDTO>> listarMisCasos() {
        return ResponseEntity.ok(casoService.listarVinculadosPersonaAutorizado(currentUser.personaId()));
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

    // Listar todos los casos vinculados y autorizados de una persona
    @GetMapping("/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorPersona(@PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarVinculadosPersonaAutorizado(idPersona));
    }

    // Listar casos autorizados por Abogado y Cliente (Uso interno)
    @GetMapping("/cliente/{idPersona}/{idCliente}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorCliente(
            @PathVariable Integer idPersona,
            @PathVariable Integer idCliente) {
        return ResponseEntity.ok(casoService.listarCasosAutorizadosPorAbogadoYCliente(idPersona, idCliente));
    }

    // Listar por caso padre
    @GetMapping("/padre/{idCaso}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorCasoPadre(@PathVariable Integer idCaso){
        return ResponseEntity.ok(casoService.listarPorCasoPadre(idCaso));
    }

    // Buscar por título (parcial) y persona
    @GetMapping("/buscar/titulo")
    public ResponseEntity<List<CasoResponseDTO>> buscarPorTitulo(
            @RequestParam Integer idPersona,
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.listarPorTitulo(query, idPersona));
    }

    // Listar por nombre de estado y persona
    @GetMapping("/estado/{estadoNombre}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorEstado(
            @PathVariable String estadoNombre,
            @PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarPorEstado(estadoNombre, idPersona));
    }

    // Listar por subtipo y persona
    @GetMapping("/subtipo/{idSubtipo}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorSubtipo(
            @PathVariable Integer idSubtipo,
            @PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarPorSubtipo(idSubtipo, idPersona));
    }

    // Filtro avanzado: Subtipo + Estado + Persona
    @GetMapping("/filtro-avanzado")
    public ResponseEntity<List<CasoResponseDTO>> filtrarAvanzado(
            @RequestParam Integer idPersona,
            @RequestParam Integer idEstado,
            @RequestParam Integer idSubtipo) {
        return ResponseEntity.ok(casoService.listarPorSubtipoAndEstado(idPersona, idEstado, idSubtipo));
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
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.superBuscador(query, idPersona));
    }

    // Listar por ID de Cliente exacto (Nueva implementación de apoyo)
    // Ejemplo: /api/casos/cliente-exacto/5/persona/2
    @GetMapping("/cliente-exacto/{idCliente}/persona/{idPersona}")
    public ResponseEntity<List<CasoResponseDTO>> listarPorClienteExacto(
            @PathVariable Integer idCliente,
            @PathVariable Integer idPersona) {
        return ResponseEntity.ok(casoService.listarPorCliente(idCliente, idPersona));
    }

    // Buscar casos por coincidencias en el Nombre o Identificación del Cliente (Texto libre)
    // Ejemplo: /api/casos/buscar/cliente?idPersona=2&query=10827
    @GetMapping("/buscar/cliente")
    public ResponseEntity<List<CasoResponseDTO>> buscarPorClienteNombreOIdentificacion(
            @RequestParam Integer idPersona,
            @RequestParam String query) {
        return ResponseEntity.ok(casoService.buscarPorNombreOIdentificacionCliente(query, idPersona));
    }
}