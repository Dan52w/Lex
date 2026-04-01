package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoCasoRequestDTO;
import com.example.lexcloud.dto.response.TipoCasoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoCasoMapper {
    TipoCasoResponseDTO toResponse(Tipo_Caso tipoCaso);

    @Mapping(target = "id", ignore = true)
    Tipo_Caso toEntity(TipoCasoRequestDTO request);

    List<TipoCasoResponseDTO> toResponseList(List<Tipo_Caso> tipoCasos);
}
