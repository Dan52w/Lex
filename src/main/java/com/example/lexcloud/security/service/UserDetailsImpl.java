package com.example.lexcloud.security.service;

import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private Integer id;
    private String email;
    private String nombreCompleto;
    private String cc;
    private String nombreRol;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    // Constructor completo
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

    public static UserDetailsImpl build(Persona persona, Login login) {
        // Suponiendo que el rol viene de la persona o el login (ej: persona.getRol().getNombre())
        String rol = (persona.getRol() != null) ? persona.getRol().getNombre() : "Cliente";

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(rol)
        );

        return new UserDetailsImpl(
                persona.getId(),
                login.getCorreo(),
                login.getPassword(),
                persona.getNombre() + " " + persona.getApellido(), // Concatenamos nombre completo
                persona.getCc(),
                rol,
                authorities
        );
    }

    // Getters para tus DTOs
    public Integer getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCc() { return cc; }
    public String getNombreRol() { return nombreRol; }

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