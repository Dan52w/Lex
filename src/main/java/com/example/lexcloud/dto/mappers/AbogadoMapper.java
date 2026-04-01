package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;
import com.example.lexcloud.entidades.Abogado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AbogadoMapper {

    // Mapeo de Salida (Hacia el Frontend)
    @Mapping(target = "nombrePersona",
            expression = "java(abogado.getPersona().getNombre() + \" \" + abogado.getPersona().getApellido())")
    @Mapping(source = "persona.cc", target = "cedula") // Útil para mostrar en tablas
    @Mapping(source = "firma.nombre", target = "nombreFirma")
    @Mapping(source = "tipo_Abogado.nombre", target = "tipoAbogado")
    AbogadoResponseDTO toResponse(Abogado abogado);

    // Mapeo de Entrada (Desde el Formulario)
    // Usamos el RequestDTO que creamos antes
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "firma", ignore = true)
    @Mapping(target = "tipo_Abogado", ignore = true)
    @Mapping(target = "agendas", ignore = true)
    Abogado toEntity(AbogadoRequestDTO request);

    // MapStruct aplicará automáticamente las reglas de 'toResponse' aquí
    List<AbogadoResponseDTO> toResponseList(List<Abogado> abogados);
}
