package com.example.lexcloud.entidades;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "tipo_casos")
public class Tipo_Caso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "nombre")
    private String nombre;

    // 'mappedBy' debe coincidir con el nombre del atributo en la clase Subtipo_Caso
    @OneToMany(mappedBy = "tipo_Caso")
    private List<Subtipo_Caso> subtipos;

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
}
