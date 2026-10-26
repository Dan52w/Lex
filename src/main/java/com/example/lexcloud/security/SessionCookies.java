package com.example.lexcloud.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * P0-B6/F1: un solo modelo de sesión en cookies httpOnly fijadas por el backend.
 * - lex_access: JWT corto. lex_refresh: token opaco rotativo.
 * - SameSite/Secure configurables por entorno: el despliegue actual es
 *   cross-site (front en Vercel, back en Render) y necesita SameSite=None +
 *   Secure=true; en local se usa Lax + Secure=false (ver
 *   application-example.properties). Nunca se devuelve el token en el cuerpo.
 * - Los nombres antiguos (jwt_token) ya no se leen ni se escriben: cualquier
 *   cookie jwt_token vieja queda inerte y el logout la borra por limpieza.
 */
@Component
public class SessionCookies {

    public static final String ACCESS_COOKIE = "lex_access";
    public static final String REFRESH_COOKIE = "lex_refresh";
    public static final String LEGACY_COOKIE = "jwt_token";

    @Value("${lexcloud.cookie.samesite:None}")
    private String sameSite;

    @Value("${lexcloud.cookie.secure:true}")
    private boolean secure;

    @Value("${lexcloud.jwt.refresh-expiration-days:14}")
    private long refreshDays;

    private ResponseCookie cookie(String nombre, String valor, long maxAgeSegundos) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSegundos)
                .build();
    }

    public void fijarSesion(HttpServletResponse response, String access, String refresh,
                            int accessMaxAgeSegundos) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS_COOKIE, access, accessMaxAgeSegundos).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(REFRESH_COOKIE, refresh, refreshDays * 24 * 60 * 60).toString());
    }

    public void limpiarSesion(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(LEGACY_COOKIE, "", 0).toString());
    }

    public String leerCookie(HttpServletRequest request, String nombre) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> nombre.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
