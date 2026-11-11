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

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${lexcloud.jwt.secret}")
    private String jwtSecret;

    @Value("${lexcloud.jwt.expiration}")
    private int jwtExpirationMs;

    // Generar el token
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key()) // JJWT detecta el algoritmo automáticamente según la llave
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Validar el token
    public boolean validateJwtToken(String authToken) {
        try {
            // En versiones nuevas se usa parser() y verifyWith()
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parse(authToken);
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
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}