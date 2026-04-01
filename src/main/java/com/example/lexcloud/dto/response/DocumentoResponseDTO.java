package com.example.lexcloud.dto.response;

public record DocumentoResponseDTO(Integer id,
                                   Integer id_Caso,
                                   String tituloCaso,
                                   String nombre,
                                   String direccion,
                                   String descripcion,
                                   String tipoDocumento) {
}
