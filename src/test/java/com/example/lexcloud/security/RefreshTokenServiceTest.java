package com.example.lexcloud.security;

import com.example.lexcloud.entidades.RefreshToken;
import com.example.lexcloud.repositorios.RefreshToken_Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0-A4 prueba 9 (revocación/rotación del refresh) a nivel de servicio.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshToken_Repository repository;

    @Test
    void crearGuardaHashNoElValor() {
        RefreshTokenService servicio = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(servicio, "refreshExpirationDays", 14L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        servicio.create(7, "valor-secreto-del-refresh");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertEquals(7, captor.getValue().getPersonaId());
        assertNotEquals("valor-secreto-del-refresh", captor.getValue().getTokenHash());
        assertEquals(64, captor.getValue().getTokenHash().length(), "Hash SHA-256 en hex");
        assertFalse(captor.getValue().getRevocado());
    }

    @Test
    void rotarRevocaElAnteriorYEmiteNuevo() {
        RefreshTokenService servicio = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(servicio, "refreshExpirationDays", 14L);
        RefreshToken viejo = new RefreshToken();
        viejo.setPersonaId(3);
        viejo.setTokenHash(RefreshTokenService.hash("viejo"));
        viejo.setExpiraEn(LocalDateTime.now().plusDays(5));
        viejo.setRevocado(false);
        when(repository.findByTokenHash(RefreshTokenService.hash("viejo")))
                .thenReturn(Optional.of(viejo));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<String> nuevo = servicio.rotate("viejo", "nuevo");
        assertTrue(nuevo.isPresent());
        assertEquals("nuevo", nuevo.get());
        assertTrue(viejo.getRevocado(), "El refresh anterior queda revocado");
        verify(repository, times(2)).save(any());
    }

    @Test
    void refreshRevocadoOExpiradoNoRota() {
        RefreshTokenService servicio = new RefreshTokenService(repository);
        RefreshToken revocado = new RefreshToken();
        revocado.setRevocado(true);
        revocado.setExpiraEn(LocalDateTime.now().plusDays(5));
        when(repository.findByTokenHash(RefreshTokenService.hash("revocado")))
                .thenReturn(Optional.of(revocado));
        assertTrue(servicio.rotate("revocado", "nuevo2").isEmpty());

        RefreshToken expirado = new RefreshToken();
        expirado.setRevocado(false);
        expirado.setExpiraEn(LocalDateTime.now().minusDays(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("expirado")))
                .thenReturn(Optional.of(expirado));
        assertTrue(servicio.rotate("expirado", "nuevo3").isEmpty());
        assertTrue(servicio.personaIdOf("desconocido").isEmpty());
    }
}
