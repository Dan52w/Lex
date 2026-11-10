package com.example.lexcloud.dto.mappers;

import com.example.lexcloud.dto.request.HistorialCambioRequestDTO;
import com.example.lexcloud.dto.response.HistorialCambioResponseDTO;
import com.example.lexcloud.entidades.Historial_Cambio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HistorialCambioMapper {
    @Mapping(source = "caso.titulo", target = "tituloCaso")
    @Mapping(target = "nombrePersona",
            expression = "java(historialCambio.getPersona().getNombre() + \" \" + historialCambio.getPersona().getApellido())")
    HistorialCambioResponseDTO toResponse(Historial_Cambio historialCambio);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caso", ignore = true)
    @Mapping(target = "persona", ignore = true)
    // S3: el DTO trae LocalDateTime y la entidad guarda LocalDate. La conversión
    // es explícita (se trunca la hora) en vez de depender de la conversión
    // implícita de MapStruct; un null queda en null y el servicio pone la fecha
    // actual. El front envía la fecha como ISO sin zona (YYYY-MM-DDTHH:mm:ss).
    @Mapping(target = "fecha", expression = "java(request.fecha() != null ? request.fecha().toLocalDate() : null)")
    Historial_Cambio toEntity(HistorialCambioRequestDTO request);

    List<HistorialCambioResponseDTO> toResponseList(List<Historial_Cambio> historialCambios);
}
