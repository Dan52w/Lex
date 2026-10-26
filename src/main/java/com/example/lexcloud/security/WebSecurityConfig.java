package com.example.lexcloud.security;

import com.example.lexcloud.security.jwt.exception.AuthEntryPointJwt;
import com.example.lexcloud.security.jwt.AuthTokenFilter;
import com.example.lexcloud.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final AuthEntryPointJwt unauthorizedHandler;
    private final JwtUtil jwtUtil;
    private final SessionCookies sessionCookies;
    private final CsrfOriginFilter csrfOriginFilter;

    /** Orígenes CORS: lista cerrada, nunca "*" con credenciales (P0-A2). */
    @Value("${lexcloud.cors.allowed-origins:http://localhost:3000,https://lex-cloud.vercel.app}")
    private String allowedOriginsConfig;

    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler, JwtUtil jwtUtil,
                             SessionCookies sessionCookies, CsrfOriginFilter csrfOriginFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtUtil = jwtUtil;
        this.sessionCookies = sessionCookies;
        this.csrfOriginFilter = csrfOriginFilter;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtil, userDetailsService, sessionCookies);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // IMPORTANTE: No usar trailing slashes en los dominios.
        // Si el front cambió de dominio, actualizar esta lista cerrada.
        config.setAllowedOrigins(Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim).toList());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);
        // P0-B6: ya no hay Bearer; el front no envía Authorization.
        config.setAllowedHeaders(Arrays.asList("Cache-Control", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthTokenFilter authTokenFilter,
                                           DaoAuthenticationProvider authProvider) throws Exception {
        http
                // CSRF: lo cubre CsrfOriginFilter (validación de Origin en
                // operaciones de cambio de estado) + SameSite de la cookie.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // P1-B4: cabeceras de seguridad. HSTS solo tiene efecto en
                // navegadores cuando el tráfico es HTTPS (en local por HTTP
                // se ignora); nosniff y frame DENY ya venían por defecto de
                // Spring Security. La CSP completa del front se coordina en
                // next.config.ts; aquí la API no sirve HTML, así que la CSP
                // es restrictiva por diseño.
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=(), payment=()")))
                // Cabeceras por defecto de Spring Security (X-Content-Type-Options:
                // nosniff, X-Frame-Options: DENY, etc.). La política completa
                // (HSTS, CSP) es P1-B4 y se coordina con el front.
                .authorizeHttpRequests(auth ->
                        auth
                                // P0-B1: /api/test/** ya NO es público (no hay
                                // controlador de test; el permiso quedaba abierto).
                                // P0-B1/B7: register y existe-correo exigen
                                // autenticación y, por @PreAuthorize, rol admin.
                                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                                .requestMatchers("/api/auth/**").authenticated()
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authProvider);
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(csrfOriginFilter, AuthTokenFilter.class);

        return http.build();
    }
}
