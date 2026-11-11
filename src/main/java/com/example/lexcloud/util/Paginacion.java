package com.example.lexcloud.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Construcción centralizada de {@link Pageable} para los listados.
 *
 * Reglas: la página nunca es negativa y el tamaño se limita a
 * {@link #TAMANO_MAXIMO} (100) para que ningún listado pueda pedir (o traer)
 * filas sin límite. El tamaño por defecto es 100, que es el tope.
 * H7: el orden por defecto es Sort.by("id") para que los listados paginados
 * (sobre todo los derivados, sin @Query con ORDER BY) tengan un orden
 * determinista entre páginas. Todas las entidades tienen propiedad `id`.
 */
public final class Paginacion {

    /** Tamaño máximo de página permitido en cualquier listado. */
    public static final int TAMANO_MAXIMO = 100;

    /** Tamaño usado cuando el cliente no indica uno (o indica uno inválido). */
    public static final int TAMANO_POR_DEFECTO = 100;

    private Paginacion() {
    }

    /**
     * Crea el {@link Pageable} con los límites aplicados.
     *
     * @param page página solicitada (0 = primera; negativos se corrigen a 0)
     * @param size tamaño solicitado (menor o igual a 0 usa el valor por defecto;
     *             mayor que {@link #TAMANO_MAXIMO} se recorta al máximo)
     */
    public static Pageable crear(int page, int size) {
        int pagina = Math.max(0, page);
        int tamano = size <= 0 ? TAMANO_POR_DEFECTO : Math.min(size, TAMANO_MAXIMO);
        return PageRequest.of(pagina, tamano, Sort.by("id"));
    }

    /** Primera página con el tamaño por defecto (la usan los listados sin parámetros). */
    public static Pageable porDefecto() {
        return PageRequest.of(0, TAMANO_POR_DEFECTO, Sort.by("id"));
    }
}
