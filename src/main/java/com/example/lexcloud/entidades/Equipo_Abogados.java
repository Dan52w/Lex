package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_abogado", nullable = false)
    private Abogado abogado;

    // 2. Referencia a la tabla Persona (Quien realiza la asignación o el miembro administrativo asociado)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    // 3. Referencia a la tabla Tipo_Relacion_Equipo
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_relacion", nullable = false)
    private Tipo_Relacion_Equipo tipoRelacion;

    // 4. Fecha de asignación
    @NotNull
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    // 5. Estado de la asignación (Activo/Inactivo)
    @NotNull
    @Column(nullable = false)
    private Boolean activo = true;

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipo_Abogados other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Equipo_Abogados.class.hashCode();
    }
}
