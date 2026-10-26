package com.example.lexcloud.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * P0-A3: auditoría mínima de seguridad. El autor es SIEMPRE el usuario del
 * token, nunca un id del DTO. No se guardan contraseñas, tokens ni contenido
 * de documentos. No hay endpoint de borrado ni de modificación: solo inserción
 * y lectura por admin (ver AuditoriaController). DDL en README-P0.md.
 */
@Entity
@Table(name = "auditoria_seguridad")
public class AuditoriaSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "id_persona")
    private Integer personaId;

    @Column(name = "id_firma")
    private Integer firmaId;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "recurso_tipo", length = 60)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private Integer recursoId;

    @Column(name = "detalle", length = 500)
    private String detalle;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getPersonaId() { return personaId; }
    public void setPersonaId(Integer personaId) { this.personaId = personaId; }
    public Integer getFirmaId() { return firmaId; }
    public void setFirmaId(Integer firmaId) { this.firmaId = firmaId; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getRecursoTipo() { return recursoTipo; }
    public void setRecursoTipo(String recursoTipo) { this.recursoTipo = recursoTipo; }
    public Integer getRecursoId() { return recursoId; }
    public void setRecursoId(Integer recursoId) { this.recursoId = recursoId; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
