package com.example.lexcloud.security;


import com.example.lexcloud.security.jwt.exception.AuthEntryPointJwt;
import com.example.lexcloud.security.jwt.AuthTokenFilter;
import com.example.lexcloud.security.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final AuthEntryPointJwt unauthorizedHandler;
    private final JwtUtil jwtUtil;

    // Solo inyectamos lo que no causa conflicto aquí
    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler, JwtUtil jwtUtil) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtUtil = jwtUtil;
    }

    // Pasamos el servicio como parámetro del Bean para que Spring lo resuelva directamente
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(@Lazy UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtil, userDetailsService);
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
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthTokenFilter authTokenFilter,
                                           DaoAuthenticationProvider authProvider) throws Exception { // Inyectamos el provider aquí
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:3000")); // Ajusta esto según tu frontend
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    config.setExposedHeaders(List.of("Authorization"));
                    return config;
                }))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                .anyRequest().authenticated()
                );

        // IMPORTANTE: Registrar el provider que configuramos arriba
        http.authenticationProvider(authProvider);

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}