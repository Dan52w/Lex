package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.GeneroRequestDTO;
import com.example.lexcloud.dto.response.GeneroResponseDTO;
import com.example.lexcloud.entidades.Genero;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper (componentModel = "spring")
public interface GeneroMapper {
    GeneroResponseDTO toResponse(Genero genero);

    @Mapping(target = "id", ignore = true)
    Genero toEntity(GeneroRequestDTO request);

    List<GeneroResponseDTO> toResponseList(List<Genero> generos);
}
