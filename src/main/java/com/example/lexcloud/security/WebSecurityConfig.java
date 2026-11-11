package com.example.lexcloud.security;

import com.example.lexcloud.security.jwt.exception.AuthEntryPointJwt;
import com.example.lexcloud.security.jwt.AuthTokenFilter;
import com.example.lexcloud.security.jwt.JwtUtil;
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

    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler, JwtUtil jwtUtil) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtil, userDetailsService);
    }

    // S3 — límite de intentos en el login (10/minuto por IP → 429).
    // Filtro propio, registrado antes del filtro JWT en la cadena.
    @Bean
    public RateLimitLoginFilter rateLimitLoginFilter() {
        return new RateLimitLoginFilter();
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

    // Definimos el Bean de CORS de forma externa para mayor claridad
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // IMPORTANTE: No usar trailing slashes en los dominios
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://lex-cloud.vercel.app"
        ));

        // S5 — PATCH: los 8 endpoints PATCH (reactivar, autorizar,
        // desautorizar, rechazar, desactivar) fallaban el preflight del
        // navegador porque no estaba en los métodos permitidos.
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);

        // Permitimos todos los headers necesarios para JWT
        config.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Accept"));

        // Exponemos el header Authorization por si decides enviarlo por ahí.
        // S5: se quitó "Set-Cookie": con Bearer-only (C10) el servidor ya no
        // crea ninguna cookie, era un residuo.
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthTokenFilter authTokenFilter,
                                           RateLimitLoginFilter rateLimitLoginFilter,
                                           DaoAuthenticationProvider authProvider) throws Exception {
        http
                // CSRF desactivado porque con Bearer-only no hay cookie de
                // sesión: un sitio malicioso no puede hacer que el navegador
                // envíe el token (vive en el cliente, no en una cookie), así
                // que no hay petición forjada que aprovechar. Si algún día
                // vuelve una cookie de autenticación, el CSRF debe reactivarse.
                .csrf(AbstractHttpConfigurer::disable)
                // Usamos el bean corsConfigurationSource definido arriba
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        // B5 — permitAll para /api/auth/login y /api/auth/register.
                        // El resto de /api/auth/** (logout, existe-correo) exige
                        // autenticación: /api/test/** ya no es público (no existe
                        // ningún controlador bajo /api/test; era una puerta latente).
                        // /register es público a nivel de filtro PORQUE el candado
                        // real vive en AuthServiceImpl.registrar (C1): solo permite
                        // registrar sin admin cuando todavía no existe NINGUNA
                        // credencial (bootstrap de la primera cuenta); en cuanto
                        // existe una, exige sesión de admin y el llamador sin
                        // sesión recibe 403. Si /register exigiera autenticación
                        // aquí, el bootstrap sería imposible: no habría cuenta
                        // con la que autenticarse.
                        auth.requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authProvider);
        // S3: el límite de login corre antes que el filtro JWT (el login no
        // lleva token) y solo actúa sobre POST /api/auth/login.
        http.addFilterBefore(rateLimitLoginFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}