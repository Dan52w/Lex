package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "personas")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
    @Column(name = "apellido", nullable = false, length = 150)
    private String apellido;
    @Column(name = "cc", nullable = false, length = 20)
    private String cc;
    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    // CAMBIO: En lugar de Integer, usamos el objeto Rol
    @ManyToOne(fetch = FetchType.LAZY) // Muchos usuarios tienen un mismo Rol
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @OneToOne(mappedBy = "persona")
    private Abogado abogado;

    @OneToOne(mappedBy = "persona")
    private Login login;

    // Casos donde esta persona aparece como "Persona Firma"
    @OneToMany(mappedBy = "persona_Firma")
    private Set<Vinculo_Caso> vinculosComoFirma = new LinkedHashSet<>();

    // Casos donde esta persona aparece como "Cliente"
    @OneToMany(mappedBy = "cliente")
    private Set<Vinculo_Caso> vinculosComoCliente = new LinkedHashSet<>();

    @OneToMany(mappedBy = "persona")
    private Set<Historial_Cambio> historial_Cambios = new LinkedHashSet<>();

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

    public void setRol(Rol idRol) {
        this.rol = idRol;
    }

    public Set<Vinculo_Caso> getVinculosComoFirma() {
        return vinculosComoFirma;
    }

    public void setVinculosComoFirma(Set<Vinculo_Caso> vinculosComoFirma) {
        this.vinculosComoFirma = vinculosComoFirma;
    }

    public Set<Vinculo_Caso> getVinculosComoCliente() {
        return vinculosComoCliente;
    }

    public void setVinculosComoCliente(Set<Vinculo_Caso> vinculosComoCliente) {
        this.vinculosComoCliente = vinculosComoCliente;
    }

    public Set<Historial_Cambio> getHistorial_Cambios() {
        return historial_Cambios;
    }

    public void setHistorial_Cambios(Set<Historial_Cambio> historial_Cambios) {
        this.historial_Cambios = historial_Cambios;
    }
}
