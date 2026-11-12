package com.example.lexcloud.security.jwt;

import com.example.lexcloud.security.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * S4 — emisor del token. Valor fijo del servicio: ningún otro emisor
     * es válido. Se valida al parsear (ver {@link #validateJwtToken}).
     */
    public static final String ISSUER = "lexcloud";

    /**
     * S4 — audiencia del token. Valor fijo del servicio: el token solo es
     * válido para la API de LexCloud. Se valida al parsear.
     */
    public static final String AUDIENCE = "lexcloud-api";

    @Value("${lexcloud.jwt.secret}")
    private String jwtSecret;

    @Value("${lexcloud.jwt.expiration}")
    private int jwtExpirationMs;

    // Generar el token
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                // H-03 — `jti` único por token: es la clave de revocación
                // (tabla tokens_revocados, migración V6). Sin él, el
                // logout no podría invalidar este token concreto.
                .id(UUID.randomUUID().toString())
                // S4 — emisor y audiencia fijos del servicio.
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key()) // JJWT detecta el algoritmo automáticamente según la llave
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validar el token
    public boolean validateJwtToken(String authToken) {
        try {
            // En versiones nuevas se usa parser() y verifyWith(). El parseo
            // verifica la firma y la expiración; los claims se comprueban
            // abajo.
            Claims claims = parseClaims(authToken);
            // S4 — `iss` y `aud` deben ser los del servicio cuando están
            // presentes. TRANSICIÓN: los tokens emitidos ANTES de este
            // cambio no llevan `iss`/`aud` (tampoco `jti`); rechazarlos
            // aquí cerraría la sesión vigente de Daniel sin avisar, así
            // que se aceptan hasta que expiren solos (máximo 24 h, según
            // lexcloud.jwt.expiration=86400000). Un token que SÍ trae
            // `iss`/`aud` con valores distintos se rechaza siempre: sin
            // el secreto nadie puede fabricar uno con los valores
            // correctos, así que la tolerancia solo cubre tokens legacy
            // auténticos. Pasada la transición, endurecer aquí exigiendo
            // `iss` y `aud` siempre (ver TokenRevocadoService).
            String issuer = claims.getIssuer();
            if (issuer != null && !ISSUER.equals(issuer)) {
                log.debug("Token JWT rechazado: issuer inesperado: {}", issuer);
                return false;
            }
            Set<String> audience = claims.getAudience();
            if (audience != null && !audience.isEmpty() && !audience.contains(AUDIENCE)) {
                log.debug("Token JWT rechazado: audience inesperada: {}", audience);
                return false;
            }
            return true;
        } catch (Exception e) {
            // B4 — sin System.out ni stack trace: solo el mensaje, en debug
            // (un token inválido/expirado es un evento rutinario, no un
            // error crítico). El detalle completo queda disponible si se
            // sube el nivel del logger de esta clase.
            log.debug("Token JWT inválido: {}", e.getMessage());
        }
        return false;
    }

    public String getUserNameFromJwtToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Claim {@code jti} del token (H-03). {@code null} en tokens legacy
     * emitidos antes de este cambio: esos tokens no son revocables.
     */
    public String getJtiFromJwtToken(String token) {
        return parseClaims(token).getId();
    }

    /** Claim {@code exp} del token (fecha de expiración). */
    public Date getExpirationFromJwtToken(String token) {
        return parseClaims(token).getExpiration();
    }
}
