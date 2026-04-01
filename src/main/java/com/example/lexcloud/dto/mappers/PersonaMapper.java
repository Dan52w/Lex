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
    PersonaResponseDTO toResponse(Persona persona);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rol", ignore = true)
    Persona toEntity(PersonaRequestDTO request);

    List<PersonaResponseDTO> toResponseList(List<Persona> personas);
}
