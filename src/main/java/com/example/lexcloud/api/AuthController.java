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

        // 3. Crear y configurar la Cookie HttpOnly
        Cookie jwtCookie = new Cookie("jwt_token", jwt);
        jwtCookie.setHttpOnly(true);   // Bloquea acceso desde JS (Protección XSS)
        jwtCookie.setSecure(false);    // Cambiar a TRUE en producción con HTTPS
        jwtCookie.setPath("/");        // Disponible para todas las rutas del back
        jwtCookie.setMaxAge(24 * 60 * 60); // Expira en 1 día (ajustar según jwtUtil)

        // Agregar la cookie a la respuesta de la cabecera
        response.addCookie(jwtCookie);

        // 4. Obtener datos del usuario
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Enviamos los datos del usuario en el body, pero el JWT viaja en la Cookie
        return ResponseEntity.ok(new AuthResponseDTO(
                "Cookie set successfully", // Mensaje informativo en lugar del token real
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