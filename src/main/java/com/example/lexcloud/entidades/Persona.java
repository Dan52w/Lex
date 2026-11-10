package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "personas")
@SQLDelete(sql = "UPDATE personas SET activo = false WHERE id = ?") // Al borrar, solo cambia el estado
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Size(max = 150)
    @Column(name = "apellido", nullable = false, length = 150)
    private String apellido;

    @NotNull
    @Size(max = 20)
    @Column(name = "cc", nullable = false, length = 20, unique = true)
    private String cc;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @NotNull
    @Size(max = 20)
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @NotNull
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @JsonIgnore
    @OneToOne(mappedBy = "persona")
    private Abogado abogado;

    @JsonIgnore
    @OneToOne(mappedBy = "persona")
    private Login login;

    @JsonIgnore
    @OneToMany(mappedBy = "personaFirma")
    private Set<Vinculo_Caso> vinculosComoFirma = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "persona")
    private Set<Historial_Cambio> historial_Cambios = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "persona")
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Abogado getAbogado() {
        return abogado;
    }

    public void setAbogado(Abogado abogado) {
        this.abogado = abogado;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Set<Vinculo_Caso> getVinculosComoFirma() {
        return vinculosComoFirma;
    }

    public void setVinculosComoFirma(Set<Vinculo_Caso> vinculosComoFirma) {
        this.vinculosComoFirma = vinculosComoFirma;
    }

    public Set<Historial_Cambio> getHistorial_Cambios() {
        return historial_Cambios;
    }

    public void setHistorial_Cambios(Set<Historial_Cambio> historial_Cambios) {
        this.historial_Cambios = historial_Cambios;
    }

    public Set<Equipo_Abogados> getEquiposAbogados() {
        return equiposAbogados;
    }

    public void setEquiposAbogados(Set<Equipo_Abogados> equiposAbogados) {
        this.equiposAbogados = equiposAbogados;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    // Igualdad por identidad de BD: dos instancias representan la misma
    // fila si tienen el mismo id. Con id null (aun no persistido) solo es
    // igual a si mismo. hashCode constante por clase: el id cambia de null
    // a valor al persistir y un hash basado en el id corromperia los Set.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persona other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Persona.class.hashCode();
    }
}
