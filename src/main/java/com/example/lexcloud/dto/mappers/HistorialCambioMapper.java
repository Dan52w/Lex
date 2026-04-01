package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
import com.example.lexcloud.entidades.Historial_Cambio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HistorialCambioMapper {
    @Mapping(source = "caso.titulo", target = "tituloCaso")
    @Mapping(target = "nombrePersona",
            expression = "java(historialCambio.getPersona().getNombre() + \" \" + historialCambio.getPersona().getApellido())")
    HistorialCambioResponseDTO toResponse(Historial_Cambio historialCambio);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caso", ignore = true)
    @Mapping(target = "persona", ignore = true)
    Historial_Cambio toEntity(HistorialCambioRequestDTO request);

    List<HistorialCambioResponseDTO> toResponseList(List<Historial_Cambio> historialCambios);
}
