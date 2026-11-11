package com.example.lexcloud.util;

/**
 * Validaciones de entrada para los métodos de escritura de los servicios (M7).
 *
 * Uso: primera línea de cada método de escritura, antes de cualquier acceso a
 * repositorio o verificación de permisos, p. ej.:
 * <pre>
 *     Integer idFirma = Validaciones.requerirId(request.idFirma(), "idFirma");
 *     String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre");
 * </pre>
 * Todas lanzan {@link IllegalArgumentException} (400) con mensaje genérico
 * sin ids ni valores; nunca devuelven {@code null}.
 */
public final class Validaciones {

    private Validaciones() {
    }

    /**
     * Exige un valor no nulo.
     *
     * @param valor valor a validar
     * @param campo nombre del campo, tal como aparece en el mensaje de error
     * @return el mismo valor, para usarlo directamente en la asignación
     * @throws IllegalArgumentException si {@code valor} es {@code null}
     */
    public static <T> T requerirNoNulo(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }

    /**
     * Exige un id no nulo. Delega en {@link #requerirNoNulo(Object, String)}.
     *
     * @param id    id a validar
     * @param campo nombre del campo, tal como aparece en el mensaje de error
     * @return el mismo id, para usarlo directamente
     * @throws IllegalArgumentException si {@code id} es {@code null}
     */
    public static Integer requerirId(Integer id, String campo) {
        return requerirNoNulo(id, campo);
    }

    /**
     * Exige un texto no nulo, no vacío y no compuesto solo de espacios en blanco.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, tal como aparece en el mensaje de error
     * @return el mismo valor, sin modificar (sin trim)
     * @throws IllegalArgumentException si {@code valor} es {@code null} o blank (tras trim)
     */
    public static String requerirNoBlank(String valor, String campo) {
        if (valor == null || valor.trim().isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }
}
