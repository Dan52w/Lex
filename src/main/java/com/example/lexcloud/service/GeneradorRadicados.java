package com.example.lexcloud.service;

import com.example.lexcloud.repositorios.Caso_Repository;
import org.springframework.stereotype.Component;

/**
 * Genera los radicados de un caso nuevo desde la secuencia de BD
 * {@code radicado_seq} (C5, lote 7).
 *
 * <p>Un solo {@code nextval} por caso: el radicado interno (10 dígitos) y
 * el judicial (23 dígitos, mismo valor con ceros a la izquierda) se
 * derivan del mismo número, así que ambos quedan vinculados entre sí y
 * nunca se consume un valor de secuencia sin usar el otro.</p>
 */
@Component
public class GeneradorRadicados {

    private final Caso_Repository casoRepository;

    public GeneradorRadicados(Caso_Repository casoRepository) {
        this.casoRepository = casoRepository;
    }

    /** Par de radicados generados desde un único valor de secuencia. */
    public record Radicados(String interno, String judicial) {
    }

    /**
     * Consume UN valor de {@code radicado_seq} y devuelve ambos radicados.
     * La secuencia arranca en 1000000000, así que el interno siempre tiene
     * 10 dígitos y el judicial 23.
     */
    public Radicados generar() {
        long numero = casoRepository.siguienteRadicado();
        return new Radicados(
                String.format("%010d", numero),
                String.format("%023d", numero));
    }
}
