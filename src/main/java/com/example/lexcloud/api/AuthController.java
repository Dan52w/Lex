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
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.correo(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateJwtToken(authentication);

        // C10 — token SOLO Bearer: ya no se crea ninguna cookie jwt_token.
        // El token viaja únicamente en el cuerpo de la respuesta y el
        // cliente debe enviarlo en el header `Authorization: Bearer <token>`
        // en cada petición. Sin cookie de sesión no hay nada que un sitio
        // malicioso pueda hacer enviar al navegador (ver WebSecurityConfig).

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
    public ResponseEntity<?> logout() {
        // C10 — con Bearer-only no hay sesión de servidor que invalidar ni
        // cookie que borrar: el cliente descarta el token por su cuenta.
        // El endpoint se mantiene (ahora exige autenticación) como punto
        // explícito de cierre de sesión del cliente.
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    @GetMapping("/existe-correo/{correo}")
    public ResponseEntity<Boolean> existeCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(authService.existeCorreo(correo.trim()));
    }
}
