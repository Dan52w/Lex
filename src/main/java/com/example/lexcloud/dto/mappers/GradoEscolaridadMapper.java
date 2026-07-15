package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.GradoEscolaridadRequestDTO;
import com.example.lexcloud.dto.response.GradoEscolaridadResponseDTO;
import com.example.lexcloud.entidades.Grado_Escolaridad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GradoEscolaridadMapper {
    GradoEscolaridadResponseDTO toResponse(Grado_Escolaridad gradoEscolaridad);

    @Mapping(target = "id", ignore = true)
    Grado_Escolaridad toEntity(GradoEscolaridadRequestDTO request);

    List<GradoEscolaridadResponseDTO> toResponseList(List<Grado_Escolaridad> gradoEscolaridades);
}
