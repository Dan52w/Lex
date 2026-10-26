package com.example.lexcloud.security;

import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Rol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Subcaso "expirado" de la prueba A4-9, en clase aparte a propósito:
 * necesita lexcloud.jwt.expiration=1000 (1 segundo) y la clase principal
 * A4 usa la expiración real de 15 minutos. Con 1 s de vida, el access
 * obtenido en el login ya está expirado al consultarlo tras una espera
 * corta, y /api/auth/me debe devolver 401 sin datos.
 *
 * Hereda de A4BaseIT (PostgreSQL de Testcontainers + API real), pero NO
 * usa la siembra completa: solo siembra un rol, una persona y su login
 * ficticios, porque aquí solo se prueba la expiración del token.
 */
@TestPropertySource(properties = "lexcloud.jwt.expiration=1000")
class A4TokenExpiracionIT extends A4BaseIT {

    @BeforeAll
    void sembrarExpiracion() {
        // Solo un usuario: esta clase no usa la siembra completa de la base.
        Rol rol = new Rol();
        rol.setNombre("ABOGADO");
        rol = rolRepository.save(rol);
        Persona persona = new Persona();
        persona.setNombre("Expiracion");
        persona.setApellido("Ficticia");
        persona.setCc("100000099");
        persona.setRol(rol);
        persona.setTelefono("3000000099");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setActivo(true);
        persona = personaRepository.save(persona);
        Login login = new Login();
        login.setPersona(persona);
        login.setCorreo("a4-expiracion@ejemplo.test");
        login.setPassword(new BCryptPasswordEncoder().encode(PASSWORD));
        loginRepository.save(login);
    }

    @Test
    void t09_tokenExpirado_devuelve401SinDatos() throws Exception {
        String cookies = login("a4-expiracion@ejemplo.test", PASSWORD);
        // Esperar a que el access (1 s de vida) expire, con margen.
        Thread.sleep(1500);
        var resp = get("/api/auth/me", cookies);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }
}
