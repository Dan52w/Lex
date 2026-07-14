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
    public ResponseEntity<List<ClienteResponseDTO>> listarPorFirma(@PathVariable Integer idFirma) {
        return ResponseEntity.ok(clienteService.listarFirma(idFirma));
    }

    // 3. Super Buscador Multi-filtro dentro de una firma
    // Ejemplo: /api/clientes/buscar?idFirma=1&filtro=Perez
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarClientes(
            @RequestParam Integer idFirma,
            @RequestParam String filtro) {
        return ResponseEntity.ok(clienteService.buscarClientes(idFirma, filtro));
    }

    // 4. Buscar por Identificación y Firma (Para pre-validaciones en el frontend)
    // Ejemplo: /api/clientes/buscar/identificacion?identificacion=102030&idFirma=1
    @GetMapping("/buscar/identificacion")
    public ResponseEntity<ClienteResponseDTO> buscarPorIdentificacion(
            @RequestParam String identificacion,
            @RequestParam Integer idFirma) {
        return clienteService.buscarPorIdentificacion(identificacion, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Buscar por ID con validación de Firma (Protección de datos inter-firma)
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @PathVariable Integer id,
            @RequestParam Integer idFirma) {
        return clienteService.buscarPorId(id, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 6. Actualizar datos de un cliente existente
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(
            @PathVariable Integer id,
            @RequestParam Integer idFirma,
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
}