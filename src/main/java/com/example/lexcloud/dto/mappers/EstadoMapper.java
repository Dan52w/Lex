package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.EstadoRequestDTO;
import com.example.lexcloud.dto.response.EstadoResponseDTO;
import com.example.lexcloud.entidades.Estado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstadoMapper {
    EstadoResponseDTO toResponse(Estado estado);

    @Mapping(target = "id", ignore = true)
    Estado toEntity(EstadoRequestDTO request);

    List<EstadoResponseDTO> toResponseList(List<Estado> estados);
}
