package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
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
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {

        // Autenticación (Spring Security hace la magia con UserDetailsServiceImpl)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.correo(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generar Token
        String jwt = jwtUtil.generateJwtToken(authentication);

        // Obtener datos del usuario autenticado para el DTO
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Aquí asumo que tu UserDetailsImpl tiene acceso a estos campos de la Persona
        return ResponseEntity.ok(new AuthResponseDTO(
                jwt,
                userDetails.getId(),
                userDetails.getNombreCompleto(), // Tendrías que añadir esto a UserDetailsImpl
                userDetails.getUsername(),
                userDetails.getCc(),             // Tendrías que añadir esto a UserDetailsImpl
                userDetails.getNombreRol(),            // Tendrías que añadir esto a UserDetailsImpl
                true                             // Si llegó aquí, es porque está activo
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        authService.registrar(registerRequest);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }
}