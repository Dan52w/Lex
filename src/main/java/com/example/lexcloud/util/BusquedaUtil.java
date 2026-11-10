package com.example.lexcloud.util;

/**
 * Utilidad para búsquedas con LIKE.
 *
 * Los repositorios usan {@code ESCAPE '\'} en sus consultas LIKE, pero eso
 * solo sirve si el término llega ya escapado: sin escapar, los caracteres
 * {@code %}, {@code _} y {@code \} que escriba el usuario seguirían actuando
 * como comodines (OWASP A03 — Inyección, vía comodín LIKE). Todos los
 * servicios que llamen a un método con LIKE deben pasar el término por aquí.
 */
public final class BusquedaUtil {

    private BusquedaUtil() {
    }

    /**
     * Neutraliza los comodines LIKE del término de búsqueda.
     * Un término {@code null} se trata como cadena vacía.
     */
    public static String escaparLike(String termino) {
        if (termino == null) {
            return "";
        }
        return termino.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
