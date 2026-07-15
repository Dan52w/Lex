package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.NivelSalarialRequestDTO;
import com.example.lexcloud.dto.response.NivelSalarialResponseDTO;
import com.example.lexcloud.entidades.Nivel_Salarial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NivelSalarialMapper {
    NivelSalarialResponseDTO toResponse(Nivel_Salarial nivelSalarial);

    @Mapping(target = "id", ignore = true)
    Nivel_Salarial toEntity(NivelSalarialRequestDTO request);

    List<NivelSalarialResponseDTO> toResponseList(List<Nivel_Salarial> nivelSalariales);
}
