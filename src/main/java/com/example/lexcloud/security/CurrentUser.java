package com.example.lexcloud.security;

import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.VinculoCaso_Repository;
import com.example.lexcloud.security.service.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * P0-B2: la identidad SIEMPRE sale del token validado, nunca del cliente.
 * Todos los servicios deben resolver persona/firma a través de aquí e ignorar
 * cualquier idFirma/idPersona que venga en URL, query o body.
 *
 * Regla de firma: la firma del usuario es la de su registro Abogado. Un
 * administrador sin registro de abogado tiene acceso global (es el rol que
 * gestiona firmas; sin esto el primer admin no podría operar). Un no-admin
 * sin registro de abogado no tiene firma y no puede operar sobre datos de
 * ninguna firma (403) hasta que un admin lo vincule a una.
 */
@Component
public class CurrentUser {

    private final Abogado_Repository abogadoRepository;
    private final Caso_Repository casoRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;

    public CurrentUser(Abogado_Repository abogadoRepository,
                       Caso_Repository casoRepository,
                       VinculoCaso_Repository vinculoCasoRepository) {
        this.abogadoRepository = abogadoRepository;
        this.casoRepository = casoRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
    }

    public UserDetailsImpl principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        return principal;
    }

    public Integer personaId() {
        return principal().getId();
    }

    public boolean isAdmin() {
        return principal().isAdmin();
    }

    /** Firma del token, o null si el usuario no tiene registro de abogado. */
    public Integer firmaId() {
        return abogadoRepository.findByPersonaId(personaId())
                .map(a -> a.getFirma().getId())
                .orElse(null);
    }

    public Integer abogadoId() {
        return abogadoRepository.findByPersonaId(personaId())
                .map(com.example.lexcloud.entidades.Abogado::getId)
                .orElse(null);
    }

    /** Firma de otra persona (para validar que un vínculo pertenece a la firma). */
    public Integer firmaOfPersona(Integer personaId) {
        return abogadoRepository.findByPersonaId(personaId)
                .map(a -> a.getFirma().getId())
                .orElse(null);
    }

    /** Firma del token o 403 si el usuario no pertenece a ninguna firma. */
    public Integer requireFirmaId() {
        if (isAdmin()) {
            return firmaId();
        }
        Integer firma = firmaId();
        if (firma == null) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        return firma;
    }

    /** ¿Puede el token operar sobre datos de esta firma? */
    public boolean canAccessFirma(Integer firmaId) {
        if (firmaId == null) {
            return false;
        }
        return isAdmin() || firmaId.equals(firmaId());
    }

    public void requireFirma(Integer firmaId) {
        if (!canAccessFirma(firmaId)) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
    }

    /**
     * Acceso a un caso: admin, o misma firma + vínculo autorizado del token.
     * Los listados por persona ya filtraban por vínculo autorizado; este
     * método cierra el hueco en buscarPorId/actualizar/desactivar/documentos/
     * agenda/historial, que no comprobaban nada.
     */
    public boolean canAccessCaso(Integer casoId) {
        if (isAdmin()) {
            return casoRepository.existsById(casoId);
        }
        Caso caso = casoRepository.findById(casoId).orElse(null);
        if (caso == null || caso.getFirma() == null || !caso.getFirma().getId().equals(firmaId())) {
            return false;
        }
        return vinculoCasoRepository.existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(personaId(), casoId);
    }

    public void requireCaso(Integer casoId) {
        if (!canAccessCaso(casoId)) {
            // 403 genérico a propósito: no revelar si el caso existe ni de
            // qué firma es (evita enumeración entre firmas).
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
    }

    public Caso requireCasoEntity(Integer casoId) {
        requireCaso(casoId);
        return casoRepository.findById(casoId)
                .orElseThrow(() -> new SecurityException("No tiene permisos para realizar esta acción"));
    }
}
