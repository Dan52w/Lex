package com.example.lexcloud.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** P1-B2: Pageable con límites: página mínima 0, tamaño por defecto 20 y
 * máximo 50. Un tamaño fuera de rango se recorta, nunca se rechaza con
 * error interno ni se devuelve todo el listado. */
public final class Pagination {
    public static final int MAX_SIZE = 50;
    public static final int DEFAULT_SIZE = 20;

    private Pagination() {}

    public static Pageable of(Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s);
    }
}
