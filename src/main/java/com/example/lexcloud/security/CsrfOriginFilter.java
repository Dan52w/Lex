package com.example.lexcloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * P0-A2: con autenticación por cookie (SameSite=None en el despliegue
 * cross-site) hace falta una defensa CSRF además de SameSite. Modelo elegido:
 * validación de Origin/Referer contra la lista cerrada de orígenes (la misma
 * de CORS) en toda operación que cambia estado (POST/PUT/PATCH/DELETE),
 * excepto /api/auth/login y /api/auth/refresh, que validan credenciales o
 * refresh en servidor y no ejecutan acciones autenticadas previas.
 * Un formulario cross-site no puede falsificar el header Origin (lo fija el
 * navegador), y un fetch cross-site con Origin no listado recibe 403 antes
 * de llegar al controlador. Las peticiones sin Origin (clientes no navegador)
 * no se bloquean: no son el vector CSRF.
 */
@Component
public class CsrfOriginFilter extends OncePerRequestFilter {

    @Value("${lexcloud.cors.allowed-origins:http://localhost:3000,https://lex-cloud.vercel.app}")
    private String allowedOriginsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();
        boolean cambiaEstado = "POST".equals(metodo) || "PUT".equals(metodo)
                || "PATCH".equals(metodo) || "DELETE".equals(metodo);
        boolean exento = ruta.equals("/api/auth/login") || ruta.equals("/api/auth/refresh");
        if (cambiaEstado && !exento) {
            String origin = request.getHeader("Origin");
            String referer = origin == null ? request.getHeader("Referer") : null;
            String aValidar = origin != null ? origin : referer;
            if (aValidar != null && !origenPermitido(aValidar)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":403,\"mensaje\":\"No tiene permisos para realizar esta acción\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean origenPermitido(String origen) {
        Set<String> permitidos = Set.copyOf(List.of(allowedOriginsConfig.split(",")));
        try {
            URI uri = URI.create(origen.trim());
            String normalizado = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            return permitidos.stream()
                    .map(String::trim)
                    .anyMatch(p -> p.equals(normalizado) || p.equals(origen.trim()));
        } catch (Exception e) {
            return false;
        }
    }
}
