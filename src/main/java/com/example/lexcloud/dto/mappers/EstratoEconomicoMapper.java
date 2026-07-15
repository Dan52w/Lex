package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.EstratoEconomicoRequestDTO;
import com.example.lexcloud.dto.response.EstratoEconomicoResponseDTO;
import com.example.lexcloud.entidades.Estrato_Economico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstratoEconomicoMapper {
    EstratoEconomicoResponseDTO toResponse(Estrato_Economico estratoEconomico);

    @Mapping(target = "id", ignore = true)
    Estrato_Economico toEntity(EstratoEconomicoRequestDTO request);

    List<EstratoEconomicoResponseDTO> toResponseList(List<Estrato_Economico> estratoEconomicos);
}
