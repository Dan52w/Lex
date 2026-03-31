package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Rol_Repository extends JpaRepository<Rol, Integer> {
    List<Rol> findAllByNombre(String nombre);
    Optional<Rol> findByNombre(String nombre);
}