package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.DocumentoRequestDTO;
import com.example.lexcloud.dto.response.DocumentoResponseDTO;
import com.example.lexcloud.entidades.Documento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentoMapper {
    @Mapping(source = "caso.titulo", target = "tituloCaso")
    @Mapping(source = "tipo_Documento.nombre", target = "tipoDocumento")
    DocumentoResponseDTO toResponse(Documento documento);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caso", ignore = true)
    @Mapping(target = "direccion", ignore = true)
    @Mapping(target = "tipo_Documento", ignore = true)
    Documento toEntity(DocumentoRequestDTO request);

    List<DocumentoResponseDTO> toResponseList(List<Documento> documentos);
}
