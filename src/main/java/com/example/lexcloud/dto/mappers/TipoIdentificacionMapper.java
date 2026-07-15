package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoIdentificacionRequestDTO;
import com.example.lexcloud.dto.response.TipoIdenficacionResponseDTO;
import com.example.lexcloud.entidades.Tipo_Identificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoIdentificacionMapper {
    TipoIdenficacionResponseDTO toResponse(Tipo_Identificacion tipoIdentificacion);

    @Mapping(target = "id", ignore = true)
    Tipo_Identificacion toEntity(TipoIdentificacionRequestDTO request);

    List<TipoIdenficacionResponseDTO> toResponseList(List<Tipo_Identificacion> tipoIdentificaciones);
}
