package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * P0-B3: las autoridades se normalizan a un único catálogo con prefijo ROLE_.
 * Catálogo: ROLE_ADMIN, ROLE_ABOGADO, ROLE_CLIENTE y ROLE_&lt;NOMBRE&gt; para
 * cualquier otro rol de la tabla roles (normalizado). Antes se usaba el nombre
 * crudo ("Abogado"), con el que hasRole() nunca funcionaba.
 * El nombre crudo se conserva en nombreRol solo para mostrar en la UI.
 */
public class UserDetailsImpl implements UserDetails {
    private Integer id;
    private String email;
    private String nombreCompleto;
    private String cc;
    private String nombreRol;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Integer id, String email, String password,
                           String nombreCompleto, String cc, String nombreRol,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.cc = cc;
        this.nombreRol = nombreRol;
        this.authorities = authorities;
    }

    /** Normaliza un nombre de rol de BD a su autoridad ROLE_*. */
    public static String normalizeRole(String nombreRol) {
        if (nombreRol == null || nombreRol.isBlank()) {
            return "ROLE_CLIENTE";
        }
        String sinAcentos = Normalizer.normalize(nombreRol.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalizado = sinAcentos.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return switch (normalizado) {
            case "ADMINISTRADOR", "ADMIN", "ADMINISTRATOR" -> "ROLE_ADMIN";
            case "ABOGADO", "LAWYER" -> "ROLE_ABOGADO";
            case "CLIENTE", "CLIENT" -> "ROLE_CLIENTE";
            default -> normalizado.startsWith("ROLE_") ? normalizado : "ROLE_" + normalizado;
        };
    }

    public static UserDetailsImpl build(Persona persona, Login login) {
        String rol = (persona.getRol() != null) ? persona.getRol().getNombre() : "Cliente";

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(normalizeRole(rol))
        );

        return new UserDetailsImpl(
                persona.getId(),
                login.getCorreo(),
                login.getPassword(),
                persona.getNombre() + " " + persona.getApellido(),
                persona.getCc(),
                rol,
                authorities
        );
    }

    public Integer getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCc() { return cc; }
    public String getNombreRol() { return nombreRol; }

    public boolean isAdmin() {
        return authorities != null && authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
