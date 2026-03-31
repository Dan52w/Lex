package com.example.lexcloud.entidades;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "firmas")
public class Firma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
    @Column(name = "nit", nullable = false, length = 50, unique = true)
    private String nit;
    @Column(name = "integrante", nullable = false)
    private Integer integrante;

    @OneToMany(mappedBy = "firma")
    private Set<Abogado> abogados = new LinkedHashSet<>();

    @OneToMany(mappedBy = "firma")
    private Set<Cliente> clientes = new LinkedHashSet<>();

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

    public Integer getIntegrante() {
        return integrante;
    }

    public void setIntegrante(Integer integrante) {
        this.integrante = integrante;
    }

    public Set<Abogado> getAbogados() {
        return abogados;
    }

    public void setAbogados(Set<Abogado> abogados) {
        this.abogados = abogados;
    }

    public Set<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(Set<Cliente> clientes) {
        this.clientes = clientes;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }
}
