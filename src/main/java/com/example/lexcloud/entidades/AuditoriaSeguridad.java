package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Fila de {@code auditoria_seguridad} (tabla creada por la migración V2).
 *
 * <p>Hasta la V5 ningún código la usaba (el comentario de V2 lo dice: la
 * tabla quedaba inerte). A partir de la V5,
 * {@code AuditoriaSeguridadService} escribe aquí: crear firma, crear admin
 * y cambiar un rol a admin.</p>
 *
 * <p>Los FK se mapean como {@code Integer} planos (no {@code @ManyToOne}):
 * la tabla usa {@code ON DELETE SET NULL} para conservar el registro aunque
 * la persona o la firma ya no existan, y una asociación JPA impediría
 * representar ese null con una entidad borrada.</p>
 */
@Entity
@Table(name = "auditoria_seguridad")
public class AuditoriaSeguridad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_persona")
    private Integer idPersona;

    @Column(name = "id_firma")
    private Integer idFirma;

    @Size(max = 60)
    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Size(max = 60)
    @Column(name = "recurso_tipo", length = 60)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private Integer recursoId;

    @Size(max = 500)
    @Column(name = "detalle", length = 500)
    private String detalle;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public Integer getIdFirma() {
        return idFirma;
    }

    public void setIdFirma(Integer idFirma) {
        this.idFirma = idFirma;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getRecursoTipo() {
        return recursoTipo;
    }

    public void setRecursoTipo(String recursoTipo) {
        this.recursoTipo = recursoTipo;
    }

    public Integer getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Integer recursoId) {
        this.recursoId = recursoId;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    // Igualdad por identidad de BD, mismo patrón que Persona/Rol.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditoriaSeguridad other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return AuditoriaSeguridad.class.hashCode();
    }
}
