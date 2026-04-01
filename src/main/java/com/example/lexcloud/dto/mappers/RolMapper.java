package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.RolRequestDTO;
import com.example.lexcloud.dto.response.RolResponseDTO;
import com.example.lexcloud.entidades.Rol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RolMapper {
    RolResponseDTO toResponse(Rol rol);

    @Mapping(target = "id", ignore = true)
    Rol toEntity(RolRequestDTO request);

    List<RolResponseDTO> toResponseList(List<Rol> roles);
}
