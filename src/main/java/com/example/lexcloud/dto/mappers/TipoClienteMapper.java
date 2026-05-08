package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoClienteRequestDTO;
import com.example.lexcloud.dto.response.TipoClienteResponseDTO;
import com.example.lexcloud.entidades.Tipo_Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoClienteMapper {
    TipoClienteResponseDTO toResponse(Tipo_Cliente tipoCliente);

    @Mapping(target = "id", ignore = true)
    Tipo_Cliente toEntity(TipoClienteRequestDTO request);

    List<TipoClienteResponseDTO> toResponseList(List<Tipo_Cliente> tipoClientes);
}
