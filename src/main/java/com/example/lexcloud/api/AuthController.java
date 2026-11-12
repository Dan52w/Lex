package com.example.lexcloud.api;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.dto.response.MeResponseDTO;
import com.example.lexcloud.security.jwt.JwtUtil;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.security.service.TokenRevocadoService;
import com.example.lexcloud.security.service.UserDetailsImpl;
import com.example.lexcloud.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final ContextoSeguridad contextoSeguridad;
    private final TokenRevocadoService tokenRevocadoService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AuthService authService,
                          ContextoSeguridad contextoSeguridad,
                          TokenRevocadoService tokenRevocadoService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.contextoSeguridad = contextoSeguridad;
        this.tokenRevocadoService = tokenRevocadoService;
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
        // Crea un recurso (credenciales): 201, no 200.
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuario registrado exitosamente");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // H-03 — el logout ahora REVOCA el token vigente: su `jti` se
        // inserta en tokens_revocados (migración V6) y AuthTokenFilter
        // rechazará cualquier petición que lo presente. Con Bearer-only
        // sigue sin haber sesión de servidor ni cookie que borrar: el
        // cliente descarta el token por su cuenta, y el servidor además
        // lo invalida. La respuesta es 200 cuando la petición llega
        // autenticada (token vigente, revocado o legacy sin `jti`): no
        // filtrar si la revocación tuvo efecto. OJO: un token YA revocado
        // o inválido no llega aquí — AuthTokenFilter lo deja sin
        // autenticar y la cadena responde 401 — así que el segundo
        // logout con el mismo token da 401, no 200.
        String jwt = parseBearerToken(request);
        if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
            String jti = jwtUtil.getJtiFromJwtToken(jwt);
            if (jti != null) {
                Integer idPersona = null;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                    idPersona = principal.getId();
                }
                tokenRevocadoService.revocar(jti, jwtUtil.getExpirationFromJwtToken(jwt), idPersona);
            }
            // Token legacy sin `jti`: no revocable; expira solo (máximo
            // 24 h). TokenRevocadoService lo registra en el log.
        }
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    /**
     * Token Bearer del header {@code Authorization} (mismo criterio que
     * AuthTokenFilter.parseJwt): solo Bearer, nunca cookie.
     */
    private String parseBearerToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @GetMapping("/existe-correo/{correo}")
    public ResponseEntity<Boolean> existeCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(authService.existeCorreo(correo.trim()));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<MeResponseDTO> me(Authentication authentication) {
        // Defensa: la ruta exige autenticación (WebSecurityConfig), pero si
        // alguna vez llega sin principal válido, 401 — nunca un NPE (500).
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // El endpoint exige autenticación (ver WebSecurityConfig), así que
        // el principal es siempre el UserDetailsImpl del token Bearer.
        // El front lo usa como guarda de ruta del dashboard y para mostrar
        // nombre/rol (ver src/lib/session.ts): sin /me válido el dashboard
        // no se renderiza.
        // (userDetails ya extraído en la guarda de arriba.)

        // idFirma: la firma del abogado vinculado a esta persona, solo si
        // el vínculo está activo (A5/N3: el filtro de activo vive en
        // ContextoSeguridad.idFirmaAutenticada). Un admin sin registro de
        // abogado (p. ej. el primer usuario) no tiene firma: null, y el
        // front lo tolera (normalizeMe lo convierte en null).
        Integer idFirma = contextoSeguridad.idFirmaAutenticada().orElse(null);

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
