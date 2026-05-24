package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.correo(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateJwtToken(authentication);

        // CAMBIO CLAVE: SameSite=None es indispensable para peticiones
        // cross-origin (frontend en Vercel → backend en Render).
        // Sin "None", el navegador descarta la cookie en peticiones cross-site
        // y el AuthTokenFilter nunca la recibe, forzando re-login.
        // Secure=true es obligatorio cuando SameSite=None.
        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", jwt)
                .httpOnly(true)
                .secure(true)          // SIEMPRE true con SameSite=None
                .sameSite("None")      // Necesario para cross-domain
                .path("/")
                .maxAge(24 * 60 * 60)  // 24 horas
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(new AuthResponseDTO(
                jwt,
                userDetails.getId(),
                userDetails.getNombreCompleto(),
                userDetails.getUsername(),
                userDetails.getCc(),
                userDetails.getNombreRol(),
                true
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequestDTO registerRequest) {
        authService.registrar(registerRequest);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Para invalidar la cookie el Set-Cookie debe usar exactamente los
        // mismos atributos que cuando se creó (mismo path, same SameSite, etc.)
        // de lo contrario el navegador no la encuentra para borrarla.
        ResponseCookie expiredCookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)  // maxAge=0 borra la cookie
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return ResponseEntity.ok("Sesión cerrada correctamente");
    }
}