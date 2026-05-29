package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record ClienteResponseDTO(Integer id,
                                 Integer idPersona,
                                 String nombre,
                                 String apellido,
                                 String nombreFirma,
                                 String telefono,
                                 String cc,
                                 String tipoCliente,
                                 LocalDate fechaVinculacion,
                                 String correo,
                                 LocalDate fechaNacimiento,
                                 boolean activo) {
}
