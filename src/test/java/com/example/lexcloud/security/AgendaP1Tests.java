package com.example.lexcloud.security;

import com.example.lexcloud.api.AgendaController;
import com.example.lexcloud.api.CasoController;
import com.example.lexcloud.dto.response.AgendaResponseDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.jwt.exception.AuthEntryPointJwt;
import com.example.lexcloud.service.AgentaService;
import com.example.lexcloud.service.AuthService;
import com.example.lexcloud.service.CasoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1 (Agenda + ruta canónica de equipo), sin BD: los servicios se mockean
 * como en P0SecurityTests. No toca los tests P0 existentes.
 *
 * Cubre la regresión que dejaba Agenda rota en el front real:
 * - POST sin idAbogado (NuevaAgendaModal ya no lo envía) debe ser
 *   aceptado por el controlador (el abogado lo deriva el servicio
 *   del token); antes el @NotNull del DTO lo convertía en 400.
 * - La respuesta incluye idAbogado, para que el PUT de edición no
 *   reenvíe idAbogado=0 (antes: 404 "Abogado no Encontrado").
 * - GET /api/casos/miembro/{idAbogado} es la ruta canónica de
 *   equipo/[id] (id de ABOGADO, no idPersona) y respeta firma/401.
 */
@WebMvcTest({AgendaController.class, CasoController.class})
@Import({WebSecurityConfig.class, CsrfOriginFilter.class, AuthEntryPointJwt.class})
class AgendaP1Tests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean AgentaService agentaService;
    @MockitoBean CasoService casoService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean AuthService authService;
    @MockitoBean RefreshTokenService refreshTokenService;
    @MockitoBean SessionCookies sessionCookies;
    @MockitoBean LoginAttemptService loginAttemptService;
    @MockitoBean AuditoriaService auditoriaService;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean UserDetailsService userDetailsService;

    private static final String AGENDA_VALIDA =
            "{\"idAbogado\":5,\"idCaso\":2,\"idTipoAgenda\":3,\"activa\":true,"
                    + "\"fechaExp\":\"2099-01-01T10:00:00\",\"descripcion\":\"Audiencia\"}";

    // Payload real del front: NuevaAgendaModal no envía idAbogado.
    private static final String AGENDA_SIN_ABOGADO =
            "{\"idCaso\":2,\"idTipoAgenda\":3,\"activa\":true,"
                    + "\"fechaExp\":\"2099-01-01T10:00:00\",\"descripcion\":\"Audiencia\"}";

    private static final AgendaResponseDTO RESPUESTA = new AgendaResponseDTO(
            7, 3, "Audiencia", true, 2, "Caso ejemplo", "Audiencia",
            LocalDateTime.of(2099, 1, 1, 10, 0), 5);

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void crearAgendaExito() throws Exception {
        Mockito.when(agentaService.guardar(Mockito.any())).thenReturn(RESPUESTA);
        mockMvc.perform(post("/api/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.idAbogado").value(5));
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void crearAgendaSinIdAbogadoAceptada() throws Exception {
        Mockito.when(agentaService.guardar(Mockito.any())).thenReturn(RESPUESTA);
        mockMvc.perform(post("/api/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDA_SIN_ABOGADO))
                .andExpect(status().isCreated());
    }

    @Test
    void crearAgendaSinSesionDa401() throws Exception {
        mockMvc.perform(post("/api/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDA_VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void crearAgendaValidacionDa400() throws Exception {
        // Sin idCaso ni idTipoAgenda, fecha pasada y descripción vacía.
        String invalida = "{\"idAbogado\":5,\"fechaExp\":\"2020-01-01T10:00:00\",\"descripcion\":\"\"}";
        mockMvc.perform(post("/api/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void buscarAgendaPorIdExito() throws Exception {
        Mockito.when(agentaService.buscarPorId(7)).thenReturn(Optional.of(RESPUESTA));
        mockMvc.perform(get("/api/agendas/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAbogado").value(5));
    }

    @Test
    void buscarAgendaPorIdSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/agendas/7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void listarAgendaOtraFirmaDa403() throws Exception {
        Mockito.when(agentaService.listarAbogado(9))
                .thenThrow(new SecurityException("No tiene permisos para realizar esta acción"));
        mockMvc.perform(get("/api/agendas/abogado/9"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void actualizarAgendaExito() throws Exception {
        Mockito.when(agentaService.actualizar(Mockito.eq(7), Mockito.any())).thenReturn(RESPUESTA);
        mockMvc.perform(put("/api/agendas/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void actualizarAgendaConIdAbogadoCeroAceptada() throws Exception {
        // Payload que reenviaba el front de edición cuando la respuesta
        // no incluía idAbogado: el controlador lo acepta (el servicio
        // conserva el abogado del recurso) en vez de rechazarlo.
        String conCero = "{\"idAbogado\":0,\"idCaso\":2,\"idTipoAgenda\":3,\"activa\":true,"
                + "\"fechaExp\":\"2099-01-01T10:00:00\",\"descripcion\":\"Audiencia\"}";
        Mockito.when(agentaService.actualizar(Mockito.eq(7), Mockito.any())).thenReturn(RESPUESTA);
        mockMvc.perform(put("/api/agendas/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conCero))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void listarCasosDeMiembroExito() throws Exception {
        CasoResponseDTO caso = new CasoResponseDTO(
                2, "Caso ejemplo", "Descripción", "ABIERTO", 1,
                "Civil", 1, "0000000002", "00000000000000000000002",
                LocalDate.of(2026, 1, 15));
        Mockito.when(casoService.listarCasosDeMiembro(5)).thenReturn(List.of(caso));
        mockMvc.perform(get("/api/casos/miembro/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void listarCasosDeMiembroSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/casos/miembro/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "abogado@ejemplo.co")
    void listarCasosDeMiembroOtraFirmaDa403() throws Exception {
        Mockito.when(casoService.listarCasosDeMiembro(9))
                .thenThrow(new SecurityException("No tiene permisos para realizar esta acción"));
        mockMvc.perform(get("/api/casos/miembro/9"))
                .andExpect(status().isForbidden());
    }
}
