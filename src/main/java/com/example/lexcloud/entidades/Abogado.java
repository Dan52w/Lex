package com.example.lexcloud.entidades;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "abogados")
public class Abogado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_firma", nullable = false)
    private Firma firma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_abogado", nullable = false)
    private Tipo_Abogado tipo_Abogado;

    @OneToMany(mappedBy = "abogado")
    private Set<Agenda> agendas = new LinkedHashSet<>();

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
}
