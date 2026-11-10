package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendas")
public class Agenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @NotNull
    @ColumnDefault("true")
    @Column(name = "activa", nullable = false)
    private Boolean activa;
    @NotNull
    @Column(name = "descripcion", nullable = false, length = Integer.MAX_VALUE)
    private String descripcion;
    @NotNull
    @Column(name = "fecha_exp", nullable = false)
    private LocalDateTime fechaExp;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_abogado", nullable = false)
    private Abogado abogado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_agenda", nullable = false)
    private Tipo_Agenda tipo_Agenda;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaExp() {
        return fechaExp;
    }

    public void setFechaExp(LocalDateTime fechaExp) {
        this.fechaExp = fechaExp;
    }

    public Abogado getAbogado() {
        return abogado;
    }

    public void setAbogado(Abogado abogado) {
        this.abogado = abogado;
    }

    public Tipo_Agenda getTipo_Agenda() {
        return tipo_Agenda;
    }

    public void setTipo_Agenda(Tipo_Agenda tipo_Agenda) {
        this.tipo_Agenda = tipo_Agenda;
    }

    public Caso getCaso() {
        return caso;
    }

    public void setCaso(Caso caso) {
        this.caso = caso;
    }

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Agenda other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Agenda.class.hashCode();
    }
}
