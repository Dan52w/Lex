package com.example.lexcloud.dto.response;

import java.time.LocalDate;

public record PersonaResponseDTO(Integer id,
                                 String nombre,
                                 String apellido,
                                 String cc,
                                 // Boolean (no boolean): Persona.activo es
                                 // anulable en BD; el primitivo haría NPE al
                                 // desempaquetar un null en el mapper.
                                 Boolean activo,
                                 Integer idRol,
                                 String nombreRol,
                                 String telefono,
                                 // B6: la entidad Persona guarda LocalDate (fecha, sin hora);
                                 // antes se exponía como LocalDateTime. El mapeo en
                                 // PersonaMapper es ahora directo y no exige ajuste.
                                 LocalDate fechaNacimiento,
                                 // V5: firma a la que pertenece la persona
                                 // (personas.id_firma, null si no tiene).
                                 Integer idFirma) {
}
