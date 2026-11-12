package com.example.lexcloud.security.jwt;

import com.example.lexcloud.security.service.TokenRevocadoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenRevocadoService tokenRevocadoService;

    // Constructor explícito
    public AuthTokenFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
                           TokenRevocadoService tokenRevocadoService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenRevocadoService = tokenRevocadoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Llamada al método que acabamos de modificar
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
                // H-03 — un token revocado (logout) no autentica aunque no
                // haya expirado: se deja la petición sin autenticación y la
                // cadena de seguridad responde 401 donde toca. Un token
                // legacy sin `jti` nunca está revocado (ver
                // TokenRevocadoService): se acepta hasta que expire.
                String jti = jwtUtil.getJtiFromJwtToken(jwt);
                if (jti != null && tokenRevocadoService.estaRevocado(jti)) {
                    logger.debug("Token JWT revocado (logout previo): no se autentica la petición");
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtUtil.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("No se puede establecer la autenticación del usuario: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * C10 — el token se lee SOLO del header {@code Authorization: Bearer
     * <token>}. El fallback a la cookie {@code jwt_token} se eliminó: con
     * Bearer-only no existe ninguna cookie de sesión. Sin header, devuelve
     * null y la petición sigue sin autenticación (el filtro no bloquea;
     * es la cadena de seguridad la que exige autenticación donde toca).
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
