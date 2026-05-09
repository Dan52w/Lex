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

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);

        // Permitimos todos los headers necesarios para JWT
        config.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Accept"));

        // Exponemos el header Authorization por si decides enviarlo por ahí
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthTokenFilter authTokenFilter,
                                           DaoAuthenticationProvider authProvider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Usamos el bean corsConfigurationSource definido arriba
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authProvider);
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}