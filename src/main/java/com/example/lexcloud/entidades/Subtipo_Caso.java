package com.example.lexcloud.entidades;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "subtipo_casos")
public class Subtipo_Caso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "nombre")
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_caso", nullable = false)
    private Tipo_Caso tipo_Caso;

    @OneToMany(mappedBy = "subtipo_Caso")
    private Set<Caso> casos = new LinkedHashSet<>();

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

    public Tipo_Caso getTipo_Caso() {
        return tipo_Caso;
    }

    public void setTipo_Caso(Tipo_Caso tipo_Caso) {
        this.tipo_Caso = tipo_Caso;
    }

    public Set<Caso> getCasos() {
        return casos;
    }

    public void setCasos(Set<Caso> casos) {
        this.casos = casos;
    }
}
