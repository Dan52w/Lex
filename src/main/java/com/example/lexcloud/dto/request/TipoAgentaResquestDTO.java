package com.example.lexcloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoAgentaResquestDTO(
        @NotBlank(message = "El nombre de la agenda no puede ser vacío")
        String nombre) {
}
