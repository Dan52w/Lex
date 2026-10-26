package com.example.lexcloud.api;

import java.util.List;

/** P1-B2: respuesta paginada. El tamaño se recorta a 50 por página en
 * Pagination; pedir una página enorme nunca devuelve más del máximo. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
