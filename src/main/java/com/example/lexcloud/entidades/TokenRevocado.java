package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Fila de {@code tokens_revocados} (tabla creada por la migración V6).
 *
 * <p>Revocación de JWT (H-03 / S2): cuando el usuario cierra sesión, el
 * {@code jti} de su token vigente se inserta aquí y
 * {@code AuthTokenFilter} rechaza cualquier petición que lo presente,
 * aunque no haya expirado todavía. Sin esta tabla el logout era solo
 * cosmético: el token seguía válido hasta su expiración.</p>
 *
 * <p>La fila solo tiene sentido hasta {@code expiraEn}: pasado ese momento
 * el parser ya rechaza el token por expirado, y
 * {@code TokenRevocadoService} purga las filas vencidas de forma
 * oportunista (en cada revocación), sin scheduler.</p>
 *
 * <p>El FK a persona se mapea como {@code Integer} plano (no
 * {@code @ManyToOne}), igual que {@code AuditoriaSeguridad}: la tabla usa
 * {@code ON DELETE SET NULL} para conservar el registro de revocación
 * aunque la persona ya no exista.</p>
 */
@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /** Claim {@code jti} del token revocado (UUID). Único: un token solo se revoca una vez. */
    @Size(max = 36)
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    /** Id de persona que revocó el token (principal del logout). Nullable. */
    @Column(name = "id_persona")
    private Integer idPersona;

    /** Expiración original del token (claim {@code exp}). */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    /** Momento de la revocación. */
    @Column(name = "revocado_en", nullable = false)
    private LocalDateTime revocadoEn;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public LocalDateTime getRevocadoEn() {
        return revocadoEn;
    }

    public void setRevocadoEn(LocalDateTime revocadoEn) {
        this.revocadoEn = revocadoEn;
    }

    // Igualdad por identidad de BD, mismo patrón que Persona/Rol.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenRevocado other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return TokenRevocado.class.hashCode();
    }
}
