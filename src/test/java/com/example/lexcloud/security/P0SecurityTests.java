package com.example.lexcloud.security;

import com.example.lexcloud.api.AuthController;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.jwt.exception.AuthEntryPointJwt;
import com.example.lexcloud.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-A4 pruebas 5, 6 y 12 a nivel de seguridad web (sin BD):
 * - Sin sesión, register y existe-correo no revelan nada (401).
 * - Con sesión pero sin rol admin, register y existe-correo dan 403.
 * Las pruebas 1-4, 8 y 9 contra API real con Postgres deben correrse en el
 * PC de Daniel (ver README-P0.md); aquí no hay Postgres ni Docker.
 */
@WebMvcTest(AuthController.class)
@Import({WebSecurityConfig.class, CsrfOriginFilter.class, AuthEntryPointJwt.class})
class P0SecurityTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean AuthService authService;
    @MockitoBean RefreshTokenService refreshTokenService;
    @MockitoBean SessionCookies sessionCookies;
    @MockitoBean LoginAttemptService loginAttemptService;
    @MockitoBean AuditoriaService auditoriaService;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean UserDetailsService userDetailsService;

    private static final String REGISTER_JSON =
            "{\"idPersona\":1,\"correo\":\"nuevo@ejemplo.co\",\"password\":\"ClaveSegura-2026\"}";

    @Test
    void registerSinSesionDa401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co", roles = "ABOGADO")
    void registerSinRolAdminDa403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void existeCorreoSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/auth/existe-correo/alguien@ejemplo.co"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co", roles = "ABOGADO")
    void existeCorreoSinRolAdminDa403() throws Exception {
        mockMvc.perform(get("/api/auth/existe-correo/alguien@ejemplo.co"))
                .andExpect(status().isForbidden());
    }
}
