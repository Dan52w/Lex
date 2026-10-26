package com.example.lexcloud.security;

import com.example.lexcloud.security.service.UserDetailsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0-A4 pruebas 5 (roles), 7 (contraseñas) y 11 (rate limiting) a nivel
 * unitario. Las pruebas de integración contra API real están pendientes
 * de correr en el PC de Daniel (sin Postgres aquí).
 */
class P0UnitTests {

    @Test
    void rolesSeNormalizanConPrefijoRole() {
        assertEquals("ROLE_ADMIN", UserDetailsImpl.normalizeRole("Administrador"));
        assertEquals("ROLE_ADMIN", UserDetailsImpl.normalizeRole("Admin"));
        assertEquals("ROLE_ABOGADO", UserDetailsImpl.normalizeRole("Abogado"));
        assertEquals("ROLE_CLIENTE", UserDetailsImpl.normalizeRole("Cliente"));
        assertEquals("ROLE_CLIENTE", UserDetailsImpl.normalizeRole(null));
        // Un rol con acentos/espacios se normaliza sin romper hasRole().
        assertEquals("ROLE_ASISTENTE_JURIDICO",
                UserDetailsImpl.normalizeRole("Asistente Jurídico"));
    }

    @Test
    void contrasenaMinima10YComunesRechazadas() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validar("corta", "a@b.co", 10));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validar("1234567890", "a@b.co", 10));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validar("password1", "a@b.co", 10));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validar("usuario@correo.co", "usuario@correo.co", 10));
        // Válida: 10+ caracteres, no común, distinta del correo.
        PasswordPolicy.validar("ClaveSegura-2026", "a@b.co", 10);
        // Admin exige 12.
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validar("ClaveSegura", "a@b.co", 12));
    }

    @Test
    void loginSeBloqueaTras5FallosYSeLiberaConExito() {
        LoginAttemptService servicio = new LoginAttemptService();
        String ip = "127.0.0.1";
        String correo = "victima@ejemplo.co";
        assertFalse(servicio.bloqueado(ip, correo));
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(ip, correo);
            assertFalse(servicio.bloqueado(ip, correo), "Aún no debe bloquear en " + (i + 1));
        }
        servicio.registrarFallo(ip, correo);
        assertTrue(servicio.bloqueado(ip, correo), "P0-B7: 5 fallos -> 429");
        // Otra cuenta desde la misma IP no está bloqueada (clave IP+cuenta).
        assertFalse(servicio.bloqueado(ip, "otra@ejemplo.co"));
        // Un éxito limpia el contador.
        servicio.registrarExito(ip, correo);
        assertFalse(servicio.bloqueado(ip, correo));
    }

    @Test
    void registroLimitadoPorIp() {
        LoginAttemptService servicio = new LoginAttemptService();
        String ip = "10.0.0.9";
        assertFalse(servicio.registroBloqueado(ip));
        for (int i = 0; i < LoginAttemptService.MAX_REGISTROS_HORA; i++) {
            servicio.registrarIntentoRegistro(ip);
        }
        assertTrue(servicio.registroBloqueado(ip));
    }
}
