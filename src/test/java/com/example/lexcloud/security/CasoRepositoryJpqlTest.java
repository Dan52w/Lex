package com.example.lexcloud.security;

import com.example.lexcloud.repositorios.Caso_Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardián de regresión del bug de arranque del 25-oct-2026 (Bad HQL
 * grammar en Caso_Repository): los ESCAPE quedaban DESPUÉS del paréntesis
 * que cierra el OR, y Spring Data JPA solo parsea las @Query al crear el
 * repositorio. Por eso los tests MockMvc (servicios mockeados) y el
 * contextLoads excluido nunca lo vieron: aquí el repositorio SÍ se
 * instancia con JPA real, así que una JPQL rota vuelve a tumbar el
 * contexto antes de cualquier aserción.
 *
 * Corre con H2 (sin Docker ni Postgres) solo para probar que las
 * consultas parsean y ejecutan; la prueba end-to-end contra PostgreSQL
 * real es A4SecurityIT.t01 (superBuscador vía HTTP).
 */
@DataJpaTest
class CasoRepositoryJpqlTest {

    @Autowired
    Caso_Repository casoRepository;

    @Test
    void superBuscador_parseaYejecutaSinErrorDeGramatica() {
        assertTrue(casoRepository.superBuscador("ficticio", 999).isEmpty());
    }

    @Test
    void buscarPorClienteNombreOIdentificacion_parseaYejecutaSinErrorDeGramatica() {
        assertTrue(casoRepository
                .buscarPorClienteNombreOIdentificacion("ficticio", 999).isEmpty());
    }
}
