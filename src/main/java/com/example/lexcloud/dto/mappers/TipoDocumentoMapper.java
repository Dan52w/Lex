package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoDocumentoRequestDTO;
import com.example.lexcloud.dto.response.TipoDocumentoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Documento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoDocumentoMapper {
    TipoDocumentoResponseDTO toResponse(Tipo_Documento tipoDocumento);

    @Mapping(target = "id", ignore = true)
    Tipo_Documento toEntity(TipoDocumentoRequestDTO request);

    List<TipoDocumentoResponseDTO> toResponseList(List<Tipo_Documento> tipoDocumentos);
}
