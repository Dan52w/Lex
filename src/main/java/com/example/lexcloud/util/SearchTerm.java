package com.example.lexcloud.util;

/** P1-B3/A4: escapa los comodines de LIKE para que % y _ del término de
 * búsqueda no se conviertan en comodines amplios. Debe usarse junto con
 * ESCAPE '\\' en las consultas JPQL (ver repositorios). */
public final class SearchTerm {
    private SearchTerm() {}

    public static String escape(String termino) {
        if (termino == null) {
            return "";
        }
        return termino
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
