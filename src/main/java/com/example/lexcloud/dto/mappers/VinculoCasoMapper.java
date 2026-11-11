package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.Vinculo_Caso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VinculoCasoMapper {
    @Mapping(source = "personaFirma.id", target = "idPersona")
    // Antes solo el primer nombre; ahora nombre + apellido como en el resto
    // de mappers, con guarda de null.
    @Mapping(target = "nombrePersona",
            expression = "java(vinculoCaso.getPersonaFirma() != null ? vinculoCaso.getPersonaFirma().getNombre() + \" \" + vinculoCaso.getPersonaFirma().getApellido() : null)")
    @Mapping(source = "cliente.id", target = "idCliente")
    // Antes solo "nombres"; ahora nombre completo del cliente.
    @Mapping(target = "nombreCliente",
            expression = "java(vinculoCaso.getCliente() != null ? (vinculoCaso.getCliente().getNombres() != null ? vinculoCaso.getCliente().getNombres() : \"\") + (vinculoCaso.getCliente().getPrimerApellido() != null ? \" \" + vinculoCaso.getCliente().getPrimerApellido() : \"\") + (vinculoCaso.getCliente().getSegundoApellido() != null ? \" \" + vinculoCaso.getCliente().getSegundoApellido() : \"\") : null)")
    @Mapping(source = "caso.id", target = "idCaso")
    @Mapping(source = "cliente.identificacion", target = "ccCliente")
    @Mapping(source = "caso.titulo", target = "tituloCaso")
    @Mapping(source = "vinculo.id", target = "idVinculo")
    @Mapping(source = "vinculo.nombre", target = "nombreVinculo")
    // M13: el DTO de respuesta CONSERVA el campo `autorizado` para no romper
    // el front; se deriva del estado (AUTORIZADO), nunca se lee de un flag.
    @Mapping(target = "autorizado", expression = "java(com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO.equals(vinculoCaso.getEstado()))")
    VinculoCasoResponseDTO toResponse(Vinculo_Caso vinculoCaso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "personaFirma", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "caso", ignore = true)
    @Mapping(target = "vinculo", ignore = true)
    Vinculo_Caso toEntity(VinculoCasoRequestDTO request);

    List<VinculoCasoResponseDTO> toResponseList(List<Vinculo_Caso> vinculoCasos);
}
