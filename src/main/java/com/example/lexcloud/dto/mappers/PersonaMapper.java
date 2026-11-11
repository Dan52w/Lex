package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.PersonaRequestDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.entidades.Persona;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonaMapper {
    @Mapping(source = "rol.nombre", target = "nombreRol")
    @Mapping(source = "rol.id", target = "idRol")
    PersonaResponseDTO toResponse(Persona persona);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rol", ignore = true)
    // V5: idFirma NO la copia el mapper: solo PersonaServiceImpl la fija,
    // tras validar el alcance (un admin de firma no puede asignar personas
    // a otra firma pasando el id en el cuerpo).
    @Mapping(target = "idFirma", ignore = true)
    Persona toEntity(PersonaRequestDTO request);

    List<PersonaResponseDTO> toResponseList(List<Persona> personas);
}
