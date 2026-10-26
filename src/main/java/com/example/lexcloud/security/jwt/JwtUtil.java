package com.example.lexcloud.security.jwt;

import com.example.lexcloud.security.service.UserDetailsImpl;
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
import java.util.UUID;

/**
 * P0-B6: un solo modelo de sesión.
 * - Access token: JWT corto (15-30 min; por defecto 15 min si la propiedad no
 *   define otro valor), con jti para poder identificarlo en logs sin exponerlo.
 * - El refresh NO es un JWT: es un token opaco (UUID) cuyo hash SHA-256 se
 *   guarda en BD y se puede revocar y rotar (ver RefreshTokenService).
 * - Los errores de validación van al log interno, sin System.out ni
 *   printStackTrace y sin detalles al cliente (antes imprimían en consola).
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${lexcloud.jwt.secret}")
    private String jwtSecret;

    /** Expiración del access token en ms. Por defecto 15 minutos (P0-B6). */
    @Value("${lexcloud.jwt.expiration:900000}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateAccessToken(userPrincipal.getUsername());
    }

    public String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /** Token de refresh opaco; solo su hash se persiste (nunca el valor). */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
    }

    public int getAccessExpirationMs() {
        return jwtExpirationMs;
    }

    private SecretKey key() {
        // Keys.hmacShaKeyFor exige >= 32 bytes para HS256; si el secreto
        // configurado es corto, el arranque falla de forma explícita en vez
        // de firmar con una clave débil. El secreto debe rotarse si hay duda
        // de exposición (P0-B1) y vivir solo en variables de entorno.
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parse(authToken);
            return true;
        } catch (Exception e) {
            log.warn("Token de acceso inválido o expirado: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
