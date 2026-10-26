package com.example.lexcloud.security;

import com.example.lexcloud.entidades.RefreshToken;
import com.example.lexcloud.repositorios.RefreshToken_Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * P0-B6: emisión, rotación y revocación de refresh tokens.
 * Un refresh revocado, expirado o desconocido devuelve vacío (401 en el
 * controlador). Rotar = revocar el anterior y emitir uno nuevo en la misma
 * transacción, para que un refresh robado no pueda reutilizarse tras la
 * rotación legítima.
 */
@Service
public class RefreshTokenService {

    private final RefreshToken_Repository repository;

    /** Vida del refresh en días. Por defecto 14 (P0-B6). */
    @Value("${lexcloud.jwt.refresh-expiration-days:14}")
    private long refreshExpirationDays;

    public RefreshTokenService(RefreshToken_Repository repository) {
        this.repository = repository;
    }

    public static String hash(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del refresh token", e);
        }
    }

    @Transactional
    public RefreshToken create(Integer personaId, String tokenValue) {
        RefreshToken rt = new RefreshToken();
        rt.setPersonaId(personaId);
        rt.setTokenHash(hash(tokenValue));
        rt.setExpiraEn(LocalDateTime.now().plusDays(refreshExpirationDays));
        rt.setRevocado(false);
        rt.setCreadoEn(LocalDateTime.now());
        return repository.save(rt);
    }

    /** Valida y ROTA: devuelve el nuevo valor opaco o vacío si no es válido. */
    @Transactional
    public Optional<String> rotate(String oldTokenValue, String newTokenValue) {
        Optional<RefreshToken> found = repository.findByTokenHash(hash(oldTokenValue));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken old = found.get();
        if (Boolean.TRUE.equals(old.getRevocado())
                || old.getExpiraEn().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        old.setRevocado(true);
        repository.save(old);
        create(old.getPersonaId(), newTokenValue);
        return Optional.of(newTokenValue);
    }

    public Optional<Integer> personaIdOf(String tokenValue) {
        return repository.findByTokenHash(hash(tokenValue))
                .filter(rt -> !Boolean.TRUE.equals(rt.getRevocado()))
                .filter(rt -> rt.getExpiraEn().isAfter(LocalDateTime.now()))
                .map(RefreshToken::getPersonaId);
    }

    @Transactional
    public void revoke(String tokenValue) {
        repository.findByTokenHash(hash(tokenValue)).ifPresent(rt -> {
            rt.setRevocado(true);
            repository.save(rt);
        });
    }

    @Transactional
    public void revokeAllForPersona(Integer personaId) {
        repository.revokeAllByPersonaId(personaId);
    }
}
