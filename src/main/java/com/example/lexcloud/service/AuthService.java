package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.RegisterRequestDTO;

public interface AuthService {
    void registrar(RegisterRequestDTO request);
    boolean existeCorreo(String correo);
    /** Correo de la persona (Login.id == Persona.id) o vacío si no existe. */
    java.util.Optional<String> correoDePersona(Integer personaId);
}
