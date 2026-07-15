package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoRelacionEquipoRequestDTO;
import com.example.lexcloud.dto.response.TipoRelacionEquipoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Relacion_Equipo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoRelacionEquipoMapper {
    TipoRelacionEquipoResponseDTO toResponse(Tipo_Relacion_Equipo tipoRelacionEquipo);

    @Mapping(target = "id", ignore = true)
    Tipo_Relacion_Equipo toEntity(TipoRelacionEquipoRequestDTO request);

    List<TipoRelacionEquipoResponseDTO> toResponseList(List<Tipo_Relacion_Equipo> tipoRelacionEquipos);
}
