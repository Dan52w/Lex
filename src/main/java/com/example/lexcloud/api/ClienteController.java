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

    // 1. Guardar o Vincular Cliente (Lógica de Persona existente)
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> guardar(@Valid @RequestBody ClienteRequestDTO request) {
        return new ResponseEntity<>(clienteService.guardar(request), HttpStatus.CREATED);
    }

    // 2. Listar todos los clientes de una firma
    @GetMapping("/firma/{idFirma}")
    public ResponseEntity<List<ClienteResponseDTO>> listarPorFirma(@PathVariable Integer idFirma) {
        return ResponseEntity.ok(clienteService.listarFirma(idFirma));
    }

    // 3. Buscar por nombre dentro de una firma
    // Ejemplo: /api/clientes/buscar/nombre?idFirma=1&query=Juan
    @GetMapping("/buscar/nombre")
    public ResponseEntity<List<ClienteResponseDTO>> buscarPorNombre(
            @RequestParam Integer idFirma,
            @RequestParam String query) {
        return ResponseEntity.ok(clienteService.listarNombre(idFirma, query));
    }

    // 4. Buscar por CC y Firma (Para validar si ya existe antes de crear)
    @GetMapping("/buscar/cc")
    public ResponseEntity<ClienteResponseDTO> buscarPorCc(
            @RequestParam String cc,
            @RequestParam Integer idFirma) {
        return clienteService.buscarPorCc(cc, idFirma)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Buscar por ID directo del registro de cliente
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Integer id) {
        return clienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 6. Actualizar datos (Actualiza la Persona vinculada)
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    // 7. Desactivar cliente (Soft delete de la relación con la firma)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }
}