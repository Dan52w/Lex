package com.example.lexcloud.security.service;


import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.repositorios.Login_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final Persona_Repository personaRepository;
    private final Login_Repository loginRepository;

    public UserDetailsServiceImpl(Persona_Repository personaRepository, Login_Repository loginRepository) {
        this.personaRepository = personaRepository;
        this.loginRepository = loginRepository;
    }

    @Override
    @Transactional(readOnly = true) // Usamos readOnly para optimizar
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscamos las credenciales por correo
        Login login = loginRepository.findByCorreo(email)
                .orElseThrow(() -> new UsernameNotFoundException("Correo no registrado: " + email));

        // 2. Buscamos la persona asociada que esté ACTIVA
        // Si la persona existe pero activo es false, findByIdAndActivoTrue devolverá Empty
        Persona persona = personaRepository.findByIdAndActivoTrue(login.getPersona().getId())
                .orElseThrow(() -> new UsernameNotFoundException("El usuario con email " + email + " no se encuentra activo o no existe."));

        // 3. Construimos el UserDetails con la información del login
        return UserDetailsImpl.build(persona, login);
    }
}