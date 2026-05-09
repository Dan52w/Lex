package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {

        // 1. Autenticación
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.correo(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Generar Token JWT
        String jwt = jwtUtil.generateJwtToken(authentication);

        // 3. Configuración de la Cookie (Compatible con Cross-Domain)
        // Usamos ResponseCookie (de Spring) en lugar de Cookie (Servlet) para tener más control
        org.springframework.http.ResponseCookie resCookie = org.springframework.http.ResponseCookie.from("jwt_token", jwt)
                .httpOnly(true)
                .secure(true)    // OBLIGATORIO para SameSite=None
                .sameSite("None") // Permite que la cookie viaje de Render a Localhost
                .path("/")
                .maxAge(24 * 60 * 60)
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, resCookie.toString());

        // 4. Obtener datos del usuario
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // CAMBIO CRÍTICO: Enviamos 'jwt' en lugar del mensaje de texto
        return ResponseEntity.ok(new AuthResponseDTO(
                jwt, // <--- Enviamos el token real aquí
                userDetails.getId(),
                userDetails.getNombreCompleto(),
                userDetails.getUsername(),
                userDetails.getCc(),
                userDetails.getNombreRol(),
                true
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        authService.registrar(registerRequest);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Para cerrar sesión, enviamos la misma cookie con tiempo de vida 0
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Sesión cerrada correctamente");
    }
}