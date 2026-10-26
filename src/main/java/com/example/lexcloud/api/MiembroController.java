package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.MiembroAltaRequestDTO;
import com.example.lexcloud.dto.response.MiembroAltaResponseDTO;
import com.example.lexcloud.service.MiembroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P1-F4: alta de miembro en una sola llamada transaccional.
 * Reemplaza la cadena de 4 POST del front antiguo (/nuevo-miembro).
 * Solo-admin; la firma se deriva del token (ver MiembroServiceImpl).
 */
@RestController
@RequestMapping("/api/miembros")
public class MiembroController {

    private final MiembroService miembroService;

    public MiembroController(MiembroService miembroService) {
        this.miembroService = miembroService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MiembroAltaResponseDTO> crearMiembro(
            @Valid @RequestBody MiembroAltaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(miembroService.crearMiembroCompleto(request));
    }
}
