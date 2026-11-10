package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "abogados")
public class Abogado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_firma", nullable = false)
    private Firma firma;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_abogado", nullable = false)
    private Tipo_Abogado tipo_Abogado;

    @JsonIgnore
    @OneToMany(mappedBy = "abogado")
    private Set<Agenda> agendas = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "abogado")
    private Set<Equipo_Abogados> equiposAbogados = new LinkedHashSet<>();

    @NotNull
    @Size(max = 255)
    @Column(name = "tarjeta_profesional", nullable = false)
    private String tarjeta_Profesional;

    public Set<Equipo_Abogados> getEquiposAbogados() {
        return equiposAbogados;
    }

    public void setEquiposAbogados(Set<Equipo_Abogados> equiposAbogados) {
        this.equiposAbogados = equiposAbogados;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Firma getFirma() {
        return firma;
    }

    public void setFirma(Firma firma) {
        this.firma = firma;
    }

    public Tipo_Abogado getTipo_Abogado() {
        return tipo_Abogado;
    }

    public void setTipo_Abogado(Tipo_Abogado tipo_Abogado) {
        this.tipo_Abogado = tipo_Abogado;
    }

    public Set<Agenda> getAgendas() {
        return agendas;
    }

    public void setAgendas(Set<Agenda> agendas) {
        this.agendas = agendas;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public String getTarjeta_Profesional() {
        return tarjeta_Profesional;
    }

    public void setTarjeta_Profesional(String tarjeta_Profesional) {
        this.tarjeta_Profesional = tarjeta_Profesional;
    }

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Abogado other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Abogado.class.hashCode();
    }
}
