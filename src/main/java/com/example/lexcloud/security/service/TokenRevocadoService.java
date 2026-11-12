package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.TokenRevocado;
import com.example.lexcloud.repositorios.TokenRevocado_Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Revocación de JWT (H-03 / S2).
 *
 * <p>El logout inserta el {@code jti} del token vigente en
 * {@code tokens_revocados} (migración V6) y {@code AuthTokenFilter}
 * rechaza cualquier petición que presente un {@code jti} revocado.</p>
 *
 * <p>Limpieza: NO hay scheduler en el proyecto (ningún
 * {@code @EnableScheduling} / {@code @Scheduled}), así que la purga de
 * filas expiradas es oportunista: cada revocación borra primero las filas
 * cuya {@code expira_en} ya pasó. La tabla solo crece con los logouts y
 * cada logout limpia lo vencido, así que se mantiene acotada sin
 * infraestructura nueva. Un {@code jti} revocado cuya fila ya se purgó es
 * inofensivo: su token ya expiró y el parser lo rechaza igualmente.</p>
 *
 * <p>Compatibilidad (transición): un token sin {@code jti} (emitido antes
 * de este cambio) no es revocable: {@link #estaRevocado(String)} devuelve
 * {@code false} y {@link #revocar(String, Date, Integer)} no inserta nada.
 * Esos tokens expiran solos en máximo 24 h
 * ({@code lexcloud.jwt.expiration=86400000}); pasado ese plazo, todos los
 * tokens en circulación llevan {@code jti} y la revocación es total.</p>
 */
@Service
public class TokenRevocadoService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocadoService.class);

    private final TokenRevocado_Repository tokenRevocadoRepository;

    public TokenRevocadoService(TokenRevocado_Repository tokenRevocadoRepository) {
        this.tokenRevocadoRepository = tokenRevocadoRepository;
    }

    /**
     * Revoca el token identificado por {@code jti} hasta su expiración.
     * Idempotente: revocar dos veces el mismo {@code jti} no duplica la
     * fila (UNIQUE en {@code jti}) y no falla. Un {@code jti} nulo o vacío
     * (token legacy sin {@code jti}) no inserta nada.
     */
    @Transactional
    public void revocar(String jti, Date expiracion, Integer idPersona) {
        // Purga oportunista de revocaciones ya vencidas (sin scheduler).
        tokenRevocadoRepository.deleteByExpiraEnBefore(LocalDateTime.now());
        if (jti == null || jti.isBlank()) {
            log.info("Logout de token sin jti (emitido antes de la revocación): "
                    + "no se puede revocar; expirará solo.");
            return;
        }
        if (tokenRevocadoRepository.existsByJti(jti)) {
            return;
        }
        TokenRevocado revocado = new TokenRevocado();
        revocado.setJti(jti);
        revocado.setIdPersona(idPersona);
        revocado.setExpiraEn(expiracion != null
                ? LocalDateTime.ofInstant(expiracion.toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now());
        revocado.setRevocadoEn(LocalDateTime.now());
        tokenRevocadoRepository.save(revocado);
        log.info("Token revocado (jti presente, idPersona={})", idPersona);
    }

    /**
     * Verdadero si el {@code jti} está revocado. Un {@code jti} nulo o
     * vacío (token legacy) nunca está revocado: no hay fila que lo
     * represente y el token expira solo.
     */
    @Transactional(readOnly = true)
    public boolean estaRevocado(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return tokenRevocadoRepository.existsByJti(jti);
    }
}
