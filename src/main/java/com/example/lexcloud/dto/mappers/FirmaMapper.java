package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;
import com.example.lexcloud.entidades.Firma;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FirmaMapper {
    FirmaResponseDTO toResponse(Firma firma);

    @Mapping(target = "id", ignore = true)
    Firma toEntity(FirmaRequestDTO request);

    List<FirmaResponseDTO> toResponseList(List<Firma> firmas);
}