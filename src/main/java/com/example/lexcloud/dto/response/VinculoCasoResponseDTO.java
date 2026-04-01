package com.example.lexcloud.dto.response;

public record VinculoCasoResponseDTO(Integer id,
                                     Integer idPersona,
                                     String nombrePersona,
                                     Integer idCliente,
                                     String nombreCliente,
                                     Integer idCaso,
                                     String tituloCaso,
                                     Integer idVinculo,
                                     String nombreVinculo,
                                     boolean autorizado) {
}
