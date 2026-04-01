package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.entidades.Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CasoMapper {

    // 1. De Entidad a Response (Salida)
    @Mapping(source = "estado.nombre", target = "estadoNombre")
    @Mapping(source = "subtipo_Caso.nombre", target = "subtipoNombre")
    @Mapping(source = "casoPadre.titulo", target = "tituloCasoPadre")
    CasoResponseDTO toResponse(Caso caso);

    // 2. De Request a Entidad (Entrada)
    // Nota: Los IDs se suelen manejar en el Service buscando la entidad por ID antes de mapear,
    // pero puedes ignorarlos aquí para setearlos manualmente en el Service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "subtipo_Caso", ignore = true)
    Caso toEntity(CasoRequestDTO request);

    List<CasoResponseDTO> toResponseList(List<Caso> casos);
}
