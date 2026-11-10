package com.example.lexcloud.security.service;


import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.repositorios.Login_Repository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final Login_Repository loginRepository;

    public UserDetailsServiceImpl(Login_Repository loginRepository) {
        this.loginRepository = loginRepository;
    }

    @Override
    @Transactional(readOnly = true) // Usamos readOnly para optimizar
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // SVC-7: una sola consulta (correo + persona activa) en vez de dos.
        // El Login trae la Persona asociada (lazy, dentro de la transacción);
        // si el correo no existe o la persona está inactiva, el resultado es
        // el mismo: además de ahorrar una consulta, no se revela si el correo
        // existe pero la cuenta está inactiva (anti-enumeración).
        Login login = loginRepository.findByCorreoAndPersonaActiva(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Correo no registrado o usuario inactivo: " + email));

        // 3. Construimos el UserDetails con la información del login
        return UserDetailsImpl.build(login.getPersona(), login);
    }
}