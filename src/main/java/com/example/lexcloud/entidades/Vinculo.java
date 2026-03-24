package com.example.lexcloud.entidades;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "vinculos")
public class Vinculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "vinculo")
    private Set<Vinculo_Caso> vinculo_Casos = new LinkedHashSet<>();

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

    public Set<Vinculo_Caso> getVinculo_Casos() {
        return vinculo_Casos;
    }

    public void setVinculo_Casos(Set<Vinculo_Caso> vinculo_Casos) {
        this.vinculo_Casos = vinculo_Casos;
    }
}
