package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.RegisterRequestDTO;

public interface AuthService {
    void registrar(RegisterRequestDTO request);
    boolean existeCorreo(String correo);
}
