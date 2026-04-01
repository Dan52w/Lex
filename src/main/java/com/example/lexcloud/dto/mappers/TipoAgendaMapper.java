package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoAgentaResquestDTO;
import com.example.lexcloud.dto.response.TipoAgendaResponseDTO;
import com.example.lexcloud.entidades.Tipo_Agenda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoAgendaMapper {
    TipoAgendaResponseDTO toResponse(Tipo_Agenda tipoAgenda);

    @Mapping(target = "id", ignore = true)
    Tipo_Agenda toEntity(TipoAgentaResquestDTO resquest);

    List<TipoAgendaResponseDTO> toResponseList(List<Tipo_Agenda> tipoAgendas);
}
