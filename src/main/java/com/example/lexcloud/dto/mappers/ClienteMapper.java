package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "nombrePersona",
            expression = "java(cliente.getPersona().getNombre() + \" \" + cliente.getPersona().getApellido())")
    @Mapping(source = "firma.nombre", target = "nombreFirma")
    ClienteResponseDTO toResponse(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "firma", ignore = true)
    @Mapping(target = "fechaVinculacion", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Cliente toEntity(ClienteRequestDTO request);

    List<ClienteResponseDTO> toResponseList(List<Cliente> clientes);
}
