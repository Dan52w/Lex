package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.TipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Abogado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoAbogadoMapper {
    TipoAbogadoResponseDTO toResponse(Tipo_Abogado tipoAbogado);

    @Mapping(target = "id", ignore = true)
    Tipo_Abogado toEntity(TipoAbogadoRequestDTO request);

    List<TipoAbogadoResponseDTO> toResponseList(List<Tipo_Abogado> tipoAbogados);
}
