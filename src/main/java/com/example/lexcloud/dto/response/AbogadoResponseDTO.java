package com.example.lexcloud.dto.response;

public record AbogadoResponseDTO(Integer id,
                                 Integer idPersona,
                                 String nombrePersona,
                                 String cedula,
                                 Integer idFirma,
                                 String telefono,
                                 String nombreFirma,
                                 String tipoAbogado,
                                 String tarjeta_Profesional) {
}
