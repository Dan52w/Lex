package com.example.lexcloud.dto.response;

public record FirmaResponseDTO(Integer id,
                               String nombre,
                               String nit,
                               // Integer como la entidad y el RequestDTO: la
                               // respuesta lo exponía como String y el
                               // contrato POST/GET difería de tipo.
                               Integer integrante) {
}
