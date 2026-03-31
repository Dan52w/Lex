package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_vinculacion")
    private Instant fechaVinculacion;
    @ColumnDefault("true")
    @Column(name = "activo")
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona idPersona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_firma", nullable = false)
    private Firma idFirma;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Instant getFechaVinculacion() {
        return fechaVinculacion;
    }

    public void setFechaVinculacion(Instant fechaVinculacion) {
        this.fechaVinculacion = fechaVinculacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Persona getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Persona idPersona) {
        this.idPersona = idPersona;
    }

    public Firma getIdFirma() {
        return idFirma;
    }

    public void setIdFirma(Firma idFirma) {
        this.idFirma = idFirma;
    }
}
