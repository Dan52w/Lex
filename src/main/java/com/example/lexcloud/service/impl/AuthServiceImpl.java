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
        if (loginRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("La persona no existe"));

        // La persona debe estar activa para poder registrar credenciales
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("La persona está inactiva y no puede registrar credenciales");
        }

        // La persona del sistema (sembrada por la migración V1) nunca inicia sesión
        if ("0000000000".equals(persona.getCc())) {
            throw new IllegalArgumentException("La persona del sistema no puede registrar credenciales");
        }

        // La PK de logins es id_persona: una persona solo puede tener un login
        if (loginRepository.existsById(request.idPersona())) {
            throw new IllegalArgumentException("La persona ya tiene credenciales registradas");
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
}
