package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tipo_relaciones_equipo")
public class Tipo_Relacion_Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "tipoRelacion")
    private Set<Equipo_Abogados> equiposAbogados = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<Equipo_Abogados> getEquiposAbogados() {
        return equiposAbogados;
    }

    public void setEquiposAbogados(Set<Equipo_Abogados> equiposAbogados) {
        this.equiposAbogados = equiposAbogados;
    }
}