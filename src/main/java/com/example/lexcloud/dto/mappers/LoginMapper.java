package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.LoginRequestDTO;
import com.example.lexcloud.dto.response.AuthResponseDTO;
import com.example.lexcloud.entidades.Login;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoginMapper {
    @Mapping(target = "nombreCompleto",
            expression = "java(login.getPersona().getNombre() + \" \" + login.getPersona().getApellido())")
    @Mapping(source = "persona.id", target = "personaId")
    @Mapping(source = "persona.cc", target = "cc")
    @Mapping(source = "persona.rol.nombre", target = "nombreRol")
    @Mapping(source = "persona.activo", target = "activo")
    AuthResponseDTO toResponse(Login login);

    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "password", ignore = true)
    Login toEntity(LoginRequestDTO request);
}
