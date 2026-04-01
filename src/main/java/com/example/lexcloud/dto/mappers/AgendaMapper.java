package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.AgendaRequestDTO;
import com.example.lexcloud.dto.response.AgendaResponseDTO;
import com.example.lexcloud.entidades.Agenda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AgendaMapper {

    @Mapping(source = "tipo_Agenda.nombre", target = "tipoAgenda")
    @Mapping(source = "caso.titulo",  target = "nombreCaso")
    AgendaResponseDTO toResponse(Agenda agenda);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "abogado", ignore = true)
    @Mapping(target = "tipo_Agenda", ignore = true)
    @Mapping(target = "caso", ignore = true)
    Agenda toEntity(AgendaRequestDTO request);

    List<AgendaResponseDTO> toResponseList(List<Agenda> agendas);
}
