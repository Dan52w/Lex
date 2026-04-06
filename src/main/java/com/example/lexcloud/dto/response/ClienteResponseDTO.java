package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record ClienteResponseDTO(Integer id,
                                 String nombrePersona,
                                 String nombreFirma,
                                 LocalDate fechaVinculacion,
                                 boolean activo) {
}
