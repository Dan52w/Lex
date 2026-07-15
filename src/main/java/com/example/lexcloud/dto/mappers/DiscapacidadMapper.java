package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.DiscapacidadRequestDTO;
import com.example.lexcloud.dto.response.DiscapacidadResponseDTO;
import com.example.lexcloud.entidades.Discapacidad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiscapacidadMapper {
    DiscapacidadResponseDTO toResponse(Discapacidad discapacidad);

    @Mapping(target = "id", ignore = true)
    Discapacidad toEntity(DiscapacidadRequestDTO request);

    List<DiscapacidadResponseDTO> toResponseList(List<Discapacidad> discapacidades);
}
