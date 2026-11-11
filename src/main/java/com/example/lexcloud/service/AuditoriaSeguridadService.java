package com.example.lexcloud.service;

import com.example.lexcloud.entidades.AuditoriaSeguridad;
import com.example.lexcloud.repositorios.AuditoriaSeguridad_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Escritura en {@code auditoria_seguridad} (V5).
 *
 * <p>Acciones auditadas: crear firma, actualizar firma, crear admin y
 * cambiar un rol a admin. El actor es el usuario autenticado; si no hay
 * sesión (no debería pasar en estas operaciones, todas exigen admin), el
 * registro queda con {@code id_persona} NULL en vez de romper la operación.</p>
 *
 * <p><b>Fail-closed:</b> si la inserción de auditoría falla, la excepción
 * se propaga y la operación principal se revierte con ella (misma
 * transacción): una creación de firma o de admin sin constancia en
 * auditoría no debe quedar a medias. El único dato que se recorta es el
 * detalle (límite de 500 de la columna); nunca se traga el error.</p>
 */
@Service
public class AuditoriaSeguridadService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSeguridadService.class);

    /** Acción: creación de una firma (solo súper admin). */
    public static final String ACCION_CREAR_FIRMA = "CREAR_FIRMA";
    /** Acción: actualización de una firma (solo súper admin). */
    public static final String ACCION_ACTUALIZAR_FIRMA = "ACTUALIZAR_FIRMA";
    /** Acción: creación de una persona con rol admin (solo súper admin). */
    public static final String ACCION_CREAR_ADMIN = "CREAR_ADMIN";
    /** Acción: cambio del rol de una persona a un rol admin (solo súper admin). */
    public static final String ACCION_CAMBIO_ROL_A_ADMIN = "CAMBIO_ROL_A_ADMIN";

    private final AuditoriaSeguridad_Repository auditoriaRepository;
    private final ContextoSeguridad contextoSeguridad;

    public AuditoriaSeguridadService(AuditoriaSeguridad_Repository auditoriaRepository,
                                     ContextoSeguridad contextoSeguridad) {
        this.auditoriaRepository = auditoriaRepository;
        this.contextoSeguridad = contextoSeguridad;
    }

    /**
     * Registra una acción. {@code idFirma} es la firma afectada (puede ser
     * null cuando la acción no tiene firma, p. ej. crear un súper admin).
     * El detalle nunca lleva secretos (sin contraseñas ni tokens).
     */
    public void registrar(String accion, String recursoTipo, Integer recursoId,
                          Integer idFirma, String detalle) {
        Integer actor = null;
        try {
            actor = contextoSeguridad.idPersonaAutenticada();
        } catch (RuntimeException e) {
            // Sin sesión válida: se registra sin actor (id_persona NULL)
            // en vez de romper la operación por la auditoría.
            log.warn("Auditoría {} sin actor autenticado (recurso {} {})",
                    accion, recursoTipo, recursoId);
        }
        AuditoriaSeguridad registro = new AuditoriaSeguridad();
        registro.setFecha(LocalDateTime.now());
        registro.setIdPersona(actor);
        registro.setIdFirma(idFirma);
        registro.setAccion(accion);
        registro.setRecursoTipo(recursoTipo);
        registro.setRecursoId(recursoId);
        registro.setDetalle(detalle == null || detalle.length() <= 500
                ? detalle : detalle.substring(0, 500));
        auditoriaRepository.save(registro);
    }
}
