package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // 1. De Entidad a Response DTO (Salida)
    @Mapping(source = "firma.id", target = "idFirma")
    @Mapping(source = "firma.nombre", target = "nombreFirma")
    @Mapping(source = "tipoCliente.id", target = "idTipoCliente")
    @Mapping(source = "tipoCliente.nombre", target = "tipoCliente")

    // Catálogos y Caracterizaciones
    @Mapping(source = "tipoIdentificacion.id", target = "idTipoIdentificacion")
    @Mapping(source = "tipoIdentificacion.nombre", target = "tipoIdentificacion")

    @Mapping(source = "genero.id", target = "idGenero")
    @Mapping(source = "genero.nombre", target = "genero")

    @Mapping(source = "estadoCivil.id", target = "idEstadoCivil")
    @Mapping(source = "estadoCivil.nombre", target = "estadoCivil")

    @Mapping(source = "estrato.id", target = "idEstrato")
    @Mapping(source = "estrato.nombre", target = "estrato")

    @Mapping(source = "vivienda.id", target = "idVivienda")
    @Mapping(source = "vivienda.nombre", target = "vivienda")

    @Mapping(source = "nivelSalarial.id", target = "idNivelSalarial")
    @Mapping(source = "nivelSalarial.rango", target = "nivelSalarial")

    @Mapping(source = "tipoEmpleado.id", target = "idTipoEmpleado")
    @Mapping(source = "tipoEmpleado.nombre", target = "tipoEmpleado")

    @Mapping(source = "gradoEscolaridad.id", target = "idGradoEscolaridad")
    @Mapping(source = "gradoEscolaridad.nombre", target = "gradoEscolaridad")

    @Mapping(source = "discapacidad.id", target = "idDiscapacidad")
    @Mapping(source = "discapacidad.nombre", target = "discapacidad")
    ClienteResponseDTO toResponse(Cliente cliente);

    // 2. De Request DTO a Entidad (Entrada/Creación)
    // Ignoramos relaciones de entidades para que se asignen/busquen manualmente desde el Service por ID
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaVinculacion", ignore = true)
    @Mapping(target = "activo", constant = "true")
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
    Cliente toEntity(ClienteRequestDTO request);

    List<ClienteResponseDTO> toResponseList(List<Cliente> clientes);
}