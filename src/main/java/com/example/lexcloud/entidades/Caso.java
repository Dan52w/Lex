package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "casos")
public class Caso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "titulo", nullable = false)
    private String titulo;
    @Column(name = "descripcion", nullable = false, length = Integer.MAX_VALUE)
    private String descripcion;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fecha_Inicio;
    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subtipo_caso", nullable = false)
    private Subtipo_Caso subtipo_Caso;

    // 1. Relación al Padre: Muchos casos pueden tener el mismo padre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caso_padre") // FK en la tabla casos que apunta a casos(id)
    private Caso casoPadre;

    // 2. Relación a los Hijos (Opcional pero muy útil): Un padre tiene muchos hijos
    @OneToMany(mappedBy = "casoPadre", cascade = CascadeType.ALL)
    private List<Caso> subCasos = new ArrayList<>();

    @OneToMany(mappedBy = "caso")
    private Set<Vinculo_Caso> vinculo_Casos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caso")
    private Set<Agenda> agendas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caso")
    private Set<Historial_Cambio> historial_Cambios = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caso")
    private Set<Documento> documentos = new LinkedHashSet<>();

    public Caso getCasoPadre() {
        return casoPadre;
    }
    public void setCasoPadre(Caso casoPadre) {
        this.casoPadre = casoPadre;
    }

    public List<Caso> getSubCasos() {
        return subCasos;
    }
    public void setSubCasos(List<Caso> subCasos) {
        this.subCasos = subCasos;
    }

    public Set<Vinculo_Caso> getVinculo_Casos() {
        return vinculo_Casos;
    }

    public void setVinculo_Casos(Set<Vinculo_Caso> vinculo_Casos) {
        this.vinculo_Casos = vinculo_Casos;
    }

    public Set<Agenda> getAgendas() {
        return agendas;
    }

    public void setAgendas(Set<Agenda> agendas) {
        this.agendas = agendas;
    }

    public Set<Historial_Cambio> getHistorial_Cambios() {
        return historial_Cambios;
    }

    public void setHistorial_Cambios(Set<Historial_Cambio> historial_Cambios) {
        this.historial_Cambios = historial_Cambios;
    }

    public Set<Documento> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(Set<Documento> documentos) {
        this.documentos = documentos;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Subtipo_Caso getSubtipo_Caso() {
        return subtipo_Caso;
    }

    public void setSubtipo_Caso(Subtipo_Caso subtipo_Caso) {
        this.subtipo_Caso = subtipo_Caso;
    }

    public LocalDate getFecha_Inicio() {
        return fecha_Inicio;
    }

    public void setFecha_Inicio(LocalDate fecha_Inicio) {
        this.fecha_Inicio = fecha_Inicio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
