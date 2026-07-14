package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // 1. Mapeo Entidad -> ResponseDTO (Aplanamos las relaciones)
    @Mapping(source = "tipoIdentificacion.nombre", target = "tipoIdentificacion")
    @Mapping(source = "firma.nombre", target = "nombreFirma")
    @Mapping(source = "tipoCliente.nombre", target = "tipoCliente")
    ClienteResponseDTO toResponse(Cliente cliente);

    // 2. Mapeo RequestDTO -> Entidad limpia (Ignoramos las relaciones y campos autogenerados)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firma", ignore = true)
    @Mapping(target = "tipoCliente", ignore = true)
    @Mapping(target = "tipoIdentificacion", ignore = true)
    @Mapping(target = "genero", ignore = true)
    @Mapping(target = "estadoCivil", ignore = true)
    @Mapping(target = "estrato", ignore = true)
    @Mapping(target = "vivienda", ignore = true)
    @Mapping(target = "nivelSalarial", ignore = true)
    @Mapping(target = "tipoEmpleado", ignore = true)
    @Mapping(target = "gradoEscolaridad", ignore = true)
    @Mapping(target = "discapacidad", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaVinculacion", ignore = true)
    Cliente toEntity(ClienteRequestDTO request);

    // 3. Actualización Parcial de una Entidad Existente
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firma", ignore = true)
    @Mapping(target = "tipoCliente", ignore = true)
    @Mapping(target = "tipoIdentificacion", ignore = true)
    @Mapping(target = "genero", ignore = true)
    @Mapping(target = "estadoCivil", ignore = true)
    @Mapping(target = "estrato", ignore = true)
    @Mapping(target = "vivienda", ignore = true)
    @Mapping(target = "nivelSalarial", ignore = true)
    @Mapping(target = "tipoEmpleado", ignore = true)
    @Mapping(target = "gradoEscolaridad", ignore = true)
    @Mapping(target = "discapacidad", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaVinculacion", ignore = true)
    void updateClienteFromRequest(ClienteRequestDTO request, @MappingTarget Cliente cliente);

    List<ClienteResponseDTO> toResponseList(List<Cliente> clientes);
}