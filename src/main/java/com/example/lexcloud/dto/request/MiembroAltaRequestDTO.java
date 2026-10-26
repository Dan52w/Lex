package com.example.lexcloud.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * P1-F4: alta de miembro en UNA sola llamada.
 * Antes, el front hacía 4 POST encadenados (personas, abogados, logins,
 * equipo): si fallaba uno a mitad, quedaban huérfanos (persona sin login,
 * abogado sin equipo). Ahora el servicio es una única transacción: todo
 * se crea o nada se crea.
 * La firma se toma del token (admin) en el servicio, no del cliente:
 * idFirma aquí solo se admite para el admin global; en cualquier otro
 * caso se ignora y se usa la firma del token.
 */
public record MiembroAltaRequestDTO(
        @Valid @NotNull(message = "Los datos de la persona son obligatorios")
        PersonaRequestDTO persona,
        @NotNull(message = "La firma es obligatoria")
        Integer idFirma,
        @NotNull(message = "El tipo de abogado es obligatorio")
        Integer idTipoAbogado,
        @NotBlank(message = "La tarjeta profesional es obligatoria")
        @Size(max = 255, message = "La tarjeta profesional no puede superar 255 caracteres")
        String tarjetaProfesional,
        @Email(message = "Correo inválido")
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
        String password,
        // Opcional: si viene, se crea también la asignación de equipo.
        Integer idTipoRelacion,
        LocalDate fechaAsignacion
) {}
