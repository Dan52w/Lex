package com.example.lexcloud.dto.request;


import jakarta.validation.constraints.NotNull;

public record VinculoCasoRequestDTO(
        @NotNull(message = "Debe haber una persona de la firma a la cual se vincule el caso")
        Integer idPersona,
        @NotNull(message = "Debe haber un cliente la cual se vincule el caso")
        Integer idCliente,
        @NotNull(message = "Debe haber un caso, para poder generar el vinculo")
        Integer idCaso,
        @NotNull(message = "Debe indicarse el vinculo con el caso")
        Integer idVinculo,
        boolean autorizado) {
}
