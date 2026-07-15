package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.response.EquipoAbogadoResponseDTO;
import com.example.lexcloud.entidades.Equipo_Abogados;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EquipoAbogadoMapper {

    // 1. De Entidad a Response DTO
    // Extraemos los IDs de las relaciones y mapeamos los nombres descriptivos.
    // Para el abogado, navegamos de Abogado -> Persona para obtener su nombre y apellido de manera combinada.
    @Mapping(source = "abogado.id", target = "idAbogado")
    @Mapping(expression = "java(equipoAbogados.getAbogado() != null && equipoAbogados.getAbogado().getPersona() != null ? " +
            "equipoAbogados.getAbogado().getPersona().getNombre() + \" \" + equipoAbogados.getAbogado().getPersona().getApellido() : null)",
            target = "nombreAbogado")

    @Mapping(source = "persona.id", target = "idPersona")
    @Mapping(expression = "java(equipoAbogados.getPersona() != null ? " +
            "equipoAbogados.getPersona().getNombre() + \" \" + equipoAbogados.getPersona().getApellido() : null)",
            target = "nombrePersona")

    @Mapping(source = "tipoRelacion.id", target = "idTipoRelacion")
    @Mapping(source = "tipoRelacion.nombre", target = "nombreTipoRelacion") // Mapea el nombre descriptivo del rol
    EquipoAbogadoResponseDTO toResponse(Equipo_Abogados equipoAbogados);

    // 2. De Request DTO a Entidad
    // Ignoramos el id auto-generado y las entidades completas que se deben resolver en el Service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "abogado", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "tipoRelacion", ignore = true)
    Equipo_Abogados toEntity(EquipoAbogadoRequestDTO request);

    // 3. Mapeo de listas
    List<EquipoAbogadoResponseDTO> toResponseList(List<Equipo_Abogados> equipos);
}