package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;
import com.example.lexcloud.entidades.Subtipo_Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubtipoCasoMapper {
    @Mapping(source = "tipo_Caso.nombre", target = "nombreTipoCaso")
    SubtipoCasoResponseDTO toResponse(Subtipo_Caso subtipoCaso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipo_Caso", ignore = true)
    Subtipo_Caso toEntity(SubtipoCasoRequestDTO request);

    List<SubtipoCasoResponseDTO> toResponseList(List<Subtipo_Caso> subtipoCasos);
}
