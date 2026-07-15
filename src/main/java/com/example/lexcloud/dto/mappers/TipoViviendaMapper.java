package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoViviendaRequestDTO;
import com.example.lexcloud.dto.response.TipoViviendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Vivienda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoViviendaMapper {
    TipoViviendaResponseDTO toResponse(Tipo_Vivienda tipoVivienda);

    @Mapping(target = "id", ignore = true)
    Tipo_Vivienda toEntity(TipoViviendaRequestDTO request);

    List<TipoViviendaResponseDTO> toResponseList(List<Tipo_Vivienda> tipoViviendas);
}
