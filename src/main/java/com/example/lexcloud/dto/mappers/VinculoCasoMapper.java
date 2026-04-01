package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.dto.response.VinculoResponseDTO;
import com.example.lexcloud.entidades.Vinculo_Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VinculoCasoMapper {
    @Mapping(source = "personaFirma.id", target = "idPersona")
    @Mapping(source = "personaFirma.nombre", target = "nombrePersona")
    @Mapping(source = "cliente.id", target = "idCliente")
    @Mapping(source = "cliente.nombre", target = "nombreCliente")
    @Mapping(source = "caso.id", target = "idCaso")
    @Mapping(source = "caso.titulo", target = "tituloCaso")
    @Mapping(source = "vinculo.id", target = "idVinculo")
    @Mapping(source = "vinculo.nombre", target = "nombreVinculo")
    VinculoCasoResponseDTO toResponse(Vinculo_Caso vinculoCaso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "personaFirma", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "caso", ignore = true)
    @Mapping(target = "vinculo", ignore = true)
    Vinculo_Caso
    toEntity(VinculoCasoRequestDTO request);

    List<VinculoResponseDTO> toResponseList(List<Vinculo_Caso> vinculoCasos);
}
