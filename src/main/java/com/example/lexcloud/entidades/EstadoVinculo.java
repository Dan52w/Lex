package com.example.lexcloud.entidades;

/**
 * Estado de gobierno de un vínculo persona–cliente–caso (M13).
 *
 * Sustituye al antiguo flag booleano {@code autorizado}: la fuente de
 * verdad es este estado. Un vínculo nace {@code PENDIENTE} y solo pasa a
 * {@code AUTORIZADO} por la acción explícita de un administrador o del
 * abogado principal autorizado del caso; {@code RECHAZADO} es terminal
 * salvo que se vuelva a vincular.
 */
public enum EstadoVinculo {
    PENDIENTE,
    AUTORIZADO,
    RECHAZADO
}
