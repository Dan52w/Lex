package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "equipo_abogados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipo_Abogados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 1. Referencia a la tabla Abogado (Muchos registros de asignación pueden pertenecer a un Abogado)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_abogado", nullable = false)
    private Abogado abogado;

    // 2. Referencia a la tabla Persona (Quien realiza la asignación o el miembro administrativo asociado)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    // 3. Referencia a la tabla Tipo_Relacion_Equipo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_relacion", nullable = false)
    private Tipo_Relacion_Equipo tipoRelacion;

    // 4. Fecha de asignación
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    // 5. Estado de la asignación (Activo/Inactivo)
    @Column(nullable = false)
    private Boolean activo = true;
}