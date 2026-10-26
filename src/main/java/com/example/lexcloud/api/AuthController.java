package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.MeResponseDTO;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.security.LoginAttemptService;
import com.example.lexcloud.security.RefreshTokenService;
import com.example.lexcloud.security.SessionCookies;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * P0-B1/B6/B7: sesión única en cookies httpOnly.
 * - login: fija lex_access + lex_refresh, NO devuelve token en el cuerpo.
 * - register: SOLO admin autenticado (opción (a) del plan). El primer admin
 *   se crea por siembra en BD (ver README-P0.md); no hay excepción de
 *   arranque en código para no dejar una puerta de registro público.
 * - existe-correo: SOLO admin (antes público y enumeraba correos).
 * - logout: revoca el refresh en servidor y borra las cookies.
 * - refresh: rota el refresh (el anterior queda revocado).
 * - me: datos de presentación desde el token (para el front, P0-F1).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final SessionCookies sessionCookies;
    private final LoginAttemptService loginAttemptService;
    private final AuditoriaService auditoriaService;
    private final CurrentUser currentUser;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AuthService authService,
                          RefreshTokenService refreshTokenService,
                          SessionCookies sessionCookies,
                          LoginAttemptService loginAttemptService,
                          AuditoriaService auditoriaService,
                          CurrentUser currentUser) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.sessionCookies = sessionCookies;
        this.loginAttemptService = loginAttemptService;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String correo = loginRequest.correo() == null ? "" : loginRequest.correo().trim().toLowerCase();
        String ip = request.getRemoteAddr();
        if (loginAttemptService.bloqueado(ip, correo)) {
            log.warn("Login bloqueado por intentos fallidos (ip anonimizada en log de app)");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.correo(), loginRequest.password()));
        } catch (BadCredentialsException e) {
            // Mensaje genérico a propósito (no distinguir usuario inexistente
            // de contraseña incorrecta) y sin guardar la contraseña intentada.
            loginAttemptService.registrarFallo(ip, correo);
            auditoriaService.registrar(null, null, "LOGIN_FALLO", "login", null,
                    "Intento fallido para correo (hash omitido por privacidad)");
            throw e;
        }
        loginAttemptService.registrarExito(ip, correo);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String access = jwtUtil.generateAccessToken(userDetails.getUsername());
        String refresh = jwtUtil.generateRefreshTokenValue();
        refreshTokenService.create(userDetails.getId(), refresh);
        sessionCookies.fijarSesion(response, access, refresh,
                jwtUtil.getAccessExpirationMs() / 1000);
        auditoriaService.registrar(userDetails.getId(), null, "LOGIN_OK", "login",
                userDetails.getId(), null);

        return ResponseEntity.ok(meDe(userDetails));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequestDTO registerRequest,
            HttpServletRequest request) {
        if (loginAttemptService.registroBloqueado(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        loginAttemptService.registrarIntentoRegistro(request.getRemoteAddr());
        authService.registrar(registerRequest);
        auditoriaService.registrar(currentUser.personaId(), currentUser.firmaId(),
                "USUARIO_CREADO", "login", registerRequest.idPersona(), null);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refresh = sessionCookies.leerCookie(request, SessionCookies.REFRESH_COOKIE);
        if (refresh != null) {
            refreshTokenService.revoke(refresh);
        }
        sessionCookies.limpiarSesion(response);
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String oldRefresh = sessionCookies.leerCookie(request, SessionCookies.REFRESH_COOKIE);
        if (oldRefresh == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Integer> personaId = refreshTokenService.personaIdOf(oldRefresh);
        if (personaId.isEmpty()) {
            sessionCookies.limpiarSesion(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String newRefresh = jwtUtil.generateRefreshTokenValue();
        Optional<String> rotado = refreshTokenService.rotate(oldRefresh, newRefresh);
        if (rotado.isEmpty()) {
            sessionCookies.limpiarSesion(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // La persona debe seguir activa: si se desactivó, el refresh ya no
        // sirve (el access nuevo no se emite y se limpian las cookies).
        Optional<String> correo = authService.correoDePersona(personaId.get());
        if (correo.isEmpty()) {
            sessionCookies.limpiarSesion(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String newAccess = jwtUtil.generateAccessToken(correo.get());
        sessionCookies.fijarSesion(response, newAccess, newRefresh,
                jwtUtil.getAccessExpirationMs() / 1000);
        // Sin cuerpo: el cliente solo necesita las cookies nuevas y reintentar
        // la petición original (o llamar a /me para datos de presentación).
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me() {
        UserDetailsImpl principal = currentUser.principal();
        return ResponseEntity.ok(meDe(principal));
    }

    @GetMapping("/existe-correo/{correo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> existeCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(authService.existeCorreo(correo.trim()));
    }

    private MeResponseDTO meDe(UserDetailsImpl userDetails) {
        Integer firmaId = null;
        Integer abogadoId = null;
        try {
            firmaId = currentUser.firmaId();
            abogadoId = currentUser.abogadoId();
        } catch (Exception e) {
            // /me tras login: el SecurityContext ya está fijado; si la
            // resolución de firma falla, se devuelve sin firma (no es fatal).
            log.debug("No se pudo resolver la firma para /me", e);
        }
        return new MeResponseDTO(
                userDetails.getId(),
                userDetails.getNombreCompleto(),
                userDetails.getUsername(),
                userDetails.getNombreRol(),
                firmaId,
                abogadoId
        );
    }
}
