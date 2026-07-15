package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.EstadoCivilRequestDTO;
import com.example.lexcloud.dto.response.EstadoCivilResponseDTO;
import com.example.lexcloud.entidades.Estado_Civil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstadoCivilMapper {
    EstadoCivilResponseDTO toResponse(Estado_Civil estadoCivil);

    @Mapping(target = "id", ignore = true)
    Estado_Civil toEntity(EstadoCivilRequestDTO request);

    List<Estado_Civil> toResponseList(List<Estado_Civil> EstadosCiviless);
}
