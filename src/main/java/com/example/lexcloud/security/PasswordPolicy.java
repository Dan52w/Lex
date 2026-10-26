package com.example.lexcloud.security;

import java.util.Set;

/**
 * P0-B7: política de contraseñas. Mínimo 10 caracteres (12 recomendados para
 * administradores; el controlador de alta admin puede exigir 12 pasando el
 * parámetro). Se rechazan las contraseñas de la lista común evidente y la
 * contraseña igual al correo. No se guarda ni se loguea la contraseña.
 */
public final class PasswordPolicy {

    private static final Set<String> COMUNES = Set.of(
            "1234567890", "123456789", "12345678", "123456", "12345",
            "password", "password1", "contrasena", "contraseña", "qwerty",
            "qwerty123", "abc123", "111111", "000000", "admin123",
            "lexcloud", "lexcloud123");

    private PasswordPolicy() {}

    public static void validar(String password, int minimo) {
        if (password == null || password.length() < minimo) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + minimo + " caracteres");
        }
        if (COMUNES.contains(password.toLowerCase())) {
            throw new IllegalArgumentException(
                    "La contraseña es demasiado común, elige otra");
        }
    }

    public static void validar(String password, String correo, int minimo) {
        validar(password, minimo);
        if (correo != null && password.equalsIgnoreCase(correo.trim())) {
            throw new IllegalArgumentException(
                    "La contraseña no puede ser igual al correo");
        }
    }
}
