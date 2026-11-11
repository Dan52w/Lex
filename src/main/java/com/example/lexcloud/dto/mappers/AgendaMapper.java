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
    @Mapping(source = "tipo_Agenda.id", target = "idTipoAgenda")
    @Mapping(source = "caso.titulo",  target = "nombreCaso")
    @Mapping(source = "caso.id", target = "idCaso")
    // El front (EditAgendaModal) lee data.idAbogado para reenviarlo en el
    // PUT; sin este campo siempre resolvía 0 y el servicio rechazaba el id.
    @Mapping(source = "abogado.id", target = "idAbogado")
    @Mapping(expression = "java(agenda.getAbogado() != null && agenda.getAbogado().getPersona() != null ? "
            + "agenda.getAbogado().getPersona().getNombre() + \" \" + agenda.getAbogado().getPersona().getApellido() : null)",
            target = "nombreAbogado")
    AgendaResponseDTO toResponse(Agenda agenda);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "abogado", ignore = true)
    @Mapping(target = "tipo_Agenda", ignore = true)
    @Mapping(target = "caso", ignore = true)
    Agenda toEntity(AgendaRequestDTO request);

    List<AgendaResponseDTO> toResponseList(List<Agenda> agendas);
}
