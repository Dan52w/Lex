package com.example.lexcloud.dto.response;

import java.time.LocalDateTime;

public record ClienteResponseDTO(Integer id,
                                 String nombrePersona,
                                 String nombreFirma,
                                 LocalDateTime fecha_vinculación,
                                 boolean activo) {
}
