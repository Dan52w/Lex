package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;
import com.example.lexcloud.entidades.Subtipo_Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubtipoCasoMapper {
    @Mapping(source = "tipoCaso.nombre", target = "nombreTipoCaso")
    SubtipoCasoResponseDTO toResponse(Subtipo_Caso subtipoCaso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoCaso", ignore = true)
    Subtipo_Caso toEntity(SubtipoCasoRequestDTO request);

    List<SubtipoCasoResponseDTO> toResponseList(List<Subtipo_Caso> subtipoCasos);
}
