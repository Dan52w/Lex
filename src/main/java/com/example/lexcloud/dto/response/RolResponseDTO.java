package com.example.lexcloud.dto.response;

public record RolResponseDTO(Integer id,
                             String nombre,
                             // Flag real de privilegio (roles.es_admin, V4):
                             // el front filtraba roles por nombre ('ADMIN') y
                             // el privilegio nunca viajaba en la respuesta.
                             Boolean esAdmin) {
}
