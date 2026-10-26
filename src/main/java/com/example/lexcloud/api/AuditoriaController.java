package com.example.lexcloud.api;

import com.example.lexcloud.entidades.AuditoriaSeguridad;
import com.example.lexcloud.repositorios.AuditoriaSeguridad_Repository;
import com.example.lexcloud.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P0-A3: lectura de auditoría. Solo admin, y solo sobre su firma (un admin
 * sin firma ve todos los registros; es el rol que gestiona el sistema).
 * No existe endpoint de borrado ni de modificación de registros.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaSeguridad_Repository repository;
    private final CurrentUser currentUser;

    public AuditoriaController(AuditoriaSeguridad_Repository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/mi-firma")
    public PageResponse<AuditoriaSeguridad> listarMiFirma(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer firmaId = currentUser.firmaId();
        if (firmaId == null) {
            // Admin global sin firma: pagina sobre todos los registros.
            return PageResponse.of(repository.findAll(Pagination.of(page, size)));
        }
        return PageResponse.of(repository.findByFirmaIdOrderByFechaDesc(firmaId, Pagination.of(page, size)));
    }
}
