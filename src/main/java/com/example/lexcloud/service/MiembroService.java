package com.example.lexcloud.service;

import com.example.lexcloud.dto.request.MiembroAltaRequestDTO;
import com.example.lexcloud.dto.response.MiembroAltaResponseDTO;

public interface MiembroService {
    MiembroAltaResponseDTO crearMiembroCompleto(MiembroAltaRequestDTO request);
}
