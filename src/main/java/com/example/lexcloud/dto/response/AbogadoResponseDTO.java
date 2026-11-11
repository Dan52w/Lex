package com.example.lexcloud.dto.response;

public record AbogadoResponseDTO(Integer id,
                                 Integer idPersona,
                                 String nombrePersona,
                                 String cedula,
                                 Integer idFirma,
                                 String telefono,
                                 String nombreFirma,
                                 String tipoAbogado,
                                 Integer idTipoAbogado,
                                 String tarjeta_Profesional,
                                 // correo vive en Login (vía Persona.login);
                                 // la página /equipo/[id] lo muestra.
                                 String correo,
                                 Boolean activo) {
}
