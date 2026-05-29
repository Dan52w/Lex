package com.example.lexcloud.security.jwt;

import com.example.lexcloud.security.service.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

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
            //Necesitamos ver qué está fallando realmente en la consola de Railway
            System.out.println("ERROR CRÍTICO EN EL FILTRO DE AUTENTICACIÓN: " + e.getMessage());
            e.printStackTrace();
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