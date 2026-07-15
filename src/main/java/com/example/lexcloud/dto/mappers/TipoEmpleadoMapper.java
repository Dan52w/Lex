package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.TipoEmpleadoRequestDTO;
import com.example.lexcloud.dto.response.TipoEmpleadoResponseDTO;
import com.example.lexcloud.entidades.Tipo_Empleado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoEmpleadoMapper {
    TipoEmpleadoResponseDTO toResponse(Tipo_Empleado tipoEmpleado);

    @Mapping(target = "id", ignore = true)
    Tipo_Empleado toEntity(TipoEmpleadoRequestDTO request);

    List<TipoEmpleadoResponseDTO> toResponseList(List<Tipo_Empleado> tipoEmpleados);
}
