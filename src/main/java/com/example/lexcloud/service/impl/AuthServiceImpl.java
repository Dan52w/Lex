package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Login_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * Mensaje único de fallo de registro (C2): no distingue correo duplicado,
     * persona inexistente, persona inactiva, persona del sistema ni persona
     * ya con credenciales, para no servir de oráculo de enumeración. El
     * detalle específico solo queda en el log del servidor.
     */
    private static final String MENSAJE_REGISTRO_GENERICO =
            "No se pudo completar el registro. Verifique los datos e inténtelo de nuevo.";

    private final Login_Repository loginRepository;
    private final Persona_Repository personaRepository;
    private final PasswordEncoder encoder;
    private final ContextoSeguridad contextoSeguridad;

    public AuthServiceImpl(Login_Repository loginRepository,
                           Persona_Repository personaRepository,
                           PasswordEncoder encoder,
                           ContextoSeguridad contextoSeguridad) {
        this.loginRepository = loginRepository;
        this.personaRepository = personaRepository;
        this.encoder = encoder;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional // Asegura que se guarde todo bien o nada
    public void registrar(RegisterRequestDTO request) {
        // C1 — registro solo por admin, con EXCEPCIÓN DE BOOTSTRAP (decisión
        // del coordinador): si todavía no existe NINGUNA credencial en el
        // sistema (loginRepository.count() == 0), se permite el registro sin
        // administrador, porque es la única forma de crear la primera cuenta
        // (no hay ningún admin que pueda registrarla). En cuanto existe la
        // primera credencial, el portón se cierra y solo un admin puede
        // registrar. Sin esta excepción el sistema no podría usarse nunca.
        // La comprobación del count va ANTES de esAdmin(): un llamador sin
        // sesión haría que esAdmin() lanzara SecurityException, y en
        // bootstrap no hay sesión que verificar.
        if (loginRepository.count() != 0 && !contextoSeguridad.esAdmin()) {
            log.warn("Registro denegado: el usuario autenticado no es administrador");
            throw new SecurityException("No tiene permisos para registrar credenciales.");
        }

        // A6 — normalización del correo ANTES de cualquier comprobación y
        // antes de persistirlo: sin trim/lowercase, "Admin@X.com" y
        // "admin@x.com" serían dos cuentas distintas.
        String correo = request.correo() == null
                ? null : request.correo().trim().toLowerCase();
        String password = request.password();

        // A6 — defensa en profundidad: la longitud (8..72) ya la valida el
        // DTO con @Size, pero el servicio no confía solo en el controlador.
        if (correo == null || correo.isBlank()) {
            log.warn("Registro fallido: correo vacío (idPersona={})", request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            log.warn("Registro fallido: longitud de contraseña fuera de rango (idPersona={})",
                    request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        // C2 — todos los fallos devuelven el mismo mensaje genérico; el
        // motivo específico solo se registra en el log del servidor.
        if (loginRepository.existsByCorreo(correo)) {
            log.warn("Registro fallido: correo ya registrado (idPersona={})", request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElse(null);
        if (persona == null) {
            log.warn("Registro fallido: persona inexistente (idPersona={})", request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        // La persona debe estar activa para poder registrar credenciales
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            log.warn("Registro fallido: persona inactiva (idPersona={})", request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        // La persona del sistema (sembrada por la migración V1) nunca inicia sesión
        if (contextoSeguridad.esPersonaSistema(persona)) {
            log.warn("Registro fallido: persona del sistema (idPersona={})", request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        // La PK de logins es id_persona: una persona solo puede tener un login
        if (loginRepository.existsById(request.idPersona())) {
            log.warn("Registro fallido: la persona ya tiene credenciales (idPersona={})",
                    request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }

        Login login = new Login();
        login.setCorreo(correo);
        login.setPersona(persona);
        login.setPassword(encoder.encode(password));

        try {
            loginRepository.save(login);
        } catch (DataIntegrityViolationException e) {
            // A3 (auth): traduce la carrera de duplicado (dos registros
            // concurrentes del mismo correo/persona) sin filtrar información.
            log.warn("Registro fallido: violación de integridad al guardar (idPersona={})",
                    request.idPersona());
            throw new IllegalArgumentException(MENSAJE_REGISTRO_GENERICO);
        }
    }

    @Override
    public boolean existeCorreo(String correo) {
        return loginRepository.existsByCorreo(correo);
    }
}
