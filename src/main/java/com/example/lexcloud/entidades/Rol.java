package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @NotNull
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;

    /**
     * Flag de administrador (C3, columna {@code roles.es_admin} creada por la
     * migración V4 con backfill del rol ADMINISTRADOR). El privilegio de
     * administrador lo decide este flag, no el nombre del rol.
     */
    @ColumnDefault("false")
    @Column(name = "es_admin", nullable = false)
    private Boolean esAdmin = false;

    // CAMBIO: Usamos mappedBy para referenciar al atributo 'rol' de la clase Persona
    @JsonIgnore
    @OneToMany(mappedBy = "rol")
    private Set<Persona> personas = new LinkedHashSet<>();

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

    public Boolean getEsAdmin() {
        return esAdmin;
    }

    public void setEsAdmin(Boolean esAdmin) {
        this.esAdmin = esAdmin;
    }

    public Set<Persona> getPersonas() {
        return personas;
    }

    public void setPersonas(Set<Persona> personas) {
        this.personas = personas;
    }

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Rol.class.hashCode();
    }
}
