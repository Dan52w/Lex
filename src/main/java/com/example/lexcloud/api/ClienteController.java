package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // 1. Guardar un nuevo Cliente
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> guardar(@Valid @RequestBody ClienteRequestDTO request) {
        return new ResponseEntity<>(clienteService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Listar todos los clientes activos de una firma
    @GetMapping("/firma/{idFirma}")
    public ResponseEntity<List<ClienteResponseDTO>> listarPorFirma(@PathVariable Integer idFirma,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(clienteService.listarFirma(idFirma, page, size));
    }

    // 2b. P0-B2: mis clientes (canónico del front): la firma es la del
    // token, sin id en la URL. Admin: todos los clientes activos.
    // Sin firma resoluble: lista vacía (no error).
    @GetMapping("/mi-firma")
    public ResponseEntity<List<ClienteResponseDTO>> listarMiFirma(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(clienteService.listarMiFirma(page, size));
    }

    // 3. Super Buscador Multi-filtro dentro de una firma
    // Ejemplo: /api/clientes/buscar?idFirma=1&filtro=Perez
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarClientes(
            @RequestParam Integer idFirma,
            @RequestParam String filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(clienteService.buscarClientes(idFirma, filtro, page, size));
    }

    // 4. Buscar por Identificación y Firma (Para pre-validaciones en el frontend)
    // Ejemplo: /api/clientes/buscar/identificacion?identificacion=102030&idFirma=1
    // P0-B2: idFirma opcional; si no viene, el servicio lo deriva del token
    // (el admin no pasa por el check de firma; no-admin sin firma: 403).
    @GetMapping("/buscar/identificacion")
    public ResponseEntity<ClienteResponseDTO> buscarPorIdentificacion(
            @RequestParam String identificacion,
            @RequestParam(required = false) Integer idFirma) {
        return clienteService.buscarPorIdentificacion(identificacion, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Buscar por ID con validación de Firma (Protección de datos inter-firma)
    // P0-B2: idFirma opcional; si no viene, el servicio lo deriva del token.
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer idFirma) {
        return clienteService.buscarPorId(id, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 6. Actualizar datos de un cliente existente
    // P0-B2: idFirma opcional; si no viene, el servicio lo deriva del token
    // o del cuerpo (la firma del cliente no se puede cambiar).
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer idFirma,
            @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, idFirma, request));
    }

    // 7. Borrado lógico (Desactivar cliente) validando firma propietaria
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Integer id,
            @RequestParam Integer idFirma) {
        clienteService.desactivarCliente(id, idFirma);
        return ResponseEntity.noContent().build();
    }

    // 8. Reactivar cliente (espejo del desactivar: mismo idFirma por query).
    // El desactivar es DELETE /{id}; no hay PATCH /desactivar que espejear,
    // así que se usa PATCH /{id}/reactivar (ver CasoController).
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(
            @PathVariable Integer id,
            @RequestParam Integer idFirma) {
        clienteService.reactivarCliente(id, idFirma);
        return ResponseEntity.noContent().build();
    }
}