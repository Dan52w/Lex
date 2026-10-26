package com.example.lexcloud.security.jwt;

import com.example.lexcloud.security.SessionCookies;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * P0-B6: UNA sola vía de autenticación: la cookie httpOnly lex_access.
 * Se eliminó el fallback Bearer (antes convivían token en cuerpo + cookie +
 * header Authorization) y la cookie antigua jwt_token ya no se lee: los
 * tokens emitidos antes de P0 quedan automáticamente inválidos.
 * Una persona desactivada no autentica porque UserDetailsServiceImpl exige
 * persona activa; además el access es corto (15 min por defecto), así que
 * la desactivación surte efecto como máximo al expirar el access vigente.
 */
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SessionCookies sessionCookies;

    public AuthTokenFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
                           SessionCookies sessionCookies) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionCookies = sessionCookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = sessionCookies.leerCookie(request, SessionCookies.ACCESS_COOKIE);

            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
                String username = jwtUtil.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("No se puede establecer la autenticación del usuario: {}", e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }
}
