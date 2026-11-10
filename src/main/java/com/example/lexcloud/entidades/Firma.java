package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "firmas")
public class Firma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @NotNull
    @Size(max = 200)
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
    @Size(max = 50)
    @NotNull
    @Column(name = "nit", nullable = false, length = 50, unique = true)
    private String nit;
    @NotNull
    @Min(0)
    @Column(name = "integrante", nullable = false)
    private Integer integrante;

    @JsonIgnore
    @OneToMany(mappedBy = "firma")
    private Set<Abogado> abogados = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "firma")
    private Set<Cliente> clientes = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "firma")
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

    public Set<Caso> getCasos() {
        return casos;
    }

    public void setCasos(Set<Caso> casos) {
        this.casos = casos;
    }

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Firma other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Firma.class.hashCode();
    }
}
