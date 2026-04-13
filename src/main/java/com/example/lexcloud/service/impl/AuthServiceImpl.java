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

        Login login = new Login();
        login.setCorreo(request.correo());
        login.setPersona(persona);
        login.setPassword(encoder.encode(request.password()));

        loginRepository.save(login);
    }
}
