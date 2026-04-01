package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.VinculoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;
import com.example.lexcloud.entidades.Vinculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VinculoMapper {
    VinculoCasoResponseDTO toResponse(Vinculo vinculo);

    @Mapping(target = "id", ignore = true)
    Vinculo toEntity(VinculoRequestDTO request);

    List<VinculoResponseDTO> toResponseList(List<Vinculo> vinculos);
}
