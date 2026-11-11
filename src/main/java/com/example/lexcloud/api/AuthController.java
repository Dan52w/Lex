package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.dto.response.MeResponseDTO;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final Abogado_Repository abogadoRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AuthService authService,
                          Abogado_Repository abogadoRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.abogadoRepository = abogadoRepository;
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

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<MeResponseDTO> me(Authentication authentication) {
        // El endpoint exige autenticación (ver WebSecurityConfig), así que
        // el principal es siempre el UserDetailsImpl del token Bearer.
        // El front lo usa como guarda de ruta del dashboard y para mostrar
        // nombre/rol (ver src/lib/session.ts): sin /me válido el dashboard
        // no se renderiza.
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // idFirma: la firma del abogado vinculado a esta persona, solo si
        // el vínculo está activo (A5). Un admin sin registro de abogado
        // (p. ej. el primer usuario) no tiene firma: null, y el front lo
        // tolera (normalizeMe lo convierte en null).
        Integer idFirma = abogadoRepository.findByPersonaId(userDetails.getId())
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .map(a -> a.getFirma() != null ? a.getFirma().getId() : null)
                .orElse(null);

        return ResponseEntity.ok(new MeResponseDTO(
                userDetails.getId(),
                userDetails.getNombreCompleto(),
                userDetails.getUsername(),
                userDetails.getCc(),
                userDetails.getNombreRol(),
                true,
                idFirma
        ));
    }
}
