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
    // nombrePersona con guarda de null: la expresión anterior hacía NPE si
    // persona era null (MapStruct no protege las expressions).
    @Mapping(target = "nombrePersona",
            expression = "java(abogado.getPersona() != null ? abogado.getPersona().getNombre() + \" \" + abogado.getPersona().getApellido() : null)")
    @Mapping(source = "persona.cc", target = "cedula") // Útil para mostrar en tablas
    @Mapping(source = "persona.id", target = "idPersona")
    @Mapping(source = "firma.id", target = "idFirma")
    @Mapping(source = "firma.nombre", target = "nombreFirma")
    @Mapping(source = "tipo_Abogado.nombre", target = "tipoAbogado")
    @Mapping(source = "tipo_Abogado.id", target = "idTipoAbogado")
    @Mapping(source = "persona.telefono", target = "telefono")
    @Mapping(source = "persona.login.correo", target = "correo")
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
