package com.example.lexcloud.security;

import com.example.lexcloud.entidades.AuditoriaSeguridad;
import com.example.lexcloud.repositorios.AuditoriaSeguridad_Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * P0-A3: registra eventos de seguridad. REQUIRES_NEW para que el registro
 * sobreviva aunque la operación que lo generó falle después, y try/catch
 * interno para que un fallo de auditoría nunca rompa la operación principal
 * (se deja constancia en el log). Acciones canónicas:
 * LOGIN_OK, LOGIN_FALLO, USUARIO_CREADO, ROL_CAMBIADO, PERSONA_DESACTIVADA,
 * PERSONA_REACTIVADA, DOCUMENTO_DESCARGADO, DOCUMENTO_ELIMINADO,
 * VINCULO_CREADO, VINCULO_AUTORIZADO.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaSeguridad_Repository repository;

    public AuditoriaService(AuditoriaSeguridad_Repository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer personaId, Integer firmaId, String accion,
                          String recursoTipo, Integer recursoId, String detalle) {
        try {
            AuditoriaSeguridad registro = new AuditoriaSeguridad();
            registro.setFecha(LocalDateTime.now());
            registro.setPersonaId(personaId);
            registro.setFirmaId(firmaId);
            registro.setAccion(accion);
            registro.setRecursoTipo(recursoTipo);
            registro.setRecursoId(recursoId);
            // Nunca guardar secretos ni contenido: el detalle es solo referencia.
            registro.setDetalle(detalle != null && detalle.length() > 500
                    ? detalle.substring(0, 500) : detalle);
            repository.save(registro);
        } catch (Exception e) {
            log.error("No se pudo persistir el registro de auditoría {} (persona {})",
                    accion, personaId, e);
        }
    }
}
