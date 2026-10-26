package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Login_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final Login_Repository loginRepository;
    private final Persona_Repository personaRepository;
    private final PasswordEncoder encoder;

    public AuthServiceImpl(Login_Repository loginRepository,
                           Persona_Repository personaRepository,
                           PasswordEncoder encoder) {
        this.loginRepository = loginRepository;
        this.personaRepository = personaRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional // Asegura que se guarde todo bien o nada
    public void registrar(RegisterRequestDTO request) {
        // P0-B7: política de contraseña (mínimo 10; 12 si la persona a la que
        // se crea la cuenta es administradora).
        Persona personaPrevia = personaRepository.findById(request.idPersona()).orElse(null);
        int minimo = (personaPrevia != null && personaPrevia.getRol() != null
                && "ROLE_ADMIN".equals(com.example.lexcloud.security.service.UserDetailsImpl
                        .normalizeRole(personaPrevia.getRol().getNombre())))
                ? 12 : 10;
        com.example.lexcloud.security.PasswordPolicy.validar(
                request.password(), request.correo(), minimo);
        if (loginRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Persona persona = personaPrevia;
        if (persona == null) {
            throw new EntityNotFoundException("La persona no existe");
        }

        Login login = new Login();
        login.setCorreo(request.correo());
        login.setPersona(persona);
        login.setPassword(encoder.encode(request.password()));

        loginRepository.save(login);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return loginRepository.existsByCorreo(correo);
    }

    @Override
    public java.util.Optional<String> correoDePersona(Integer personaId) {
        return loginRepository.findById(personaId).map(Login::getCorreo);
    }
}
