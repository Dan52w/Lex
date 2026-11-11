package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Rol;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Rol_Repository extends JpaRepository<Rol, Integer> {
    List<Rol> findAllByNombre(String nombre);

    // M1: variante paginada del listado por nombre (mismo criterio exacto).
    List<Rol> findAllByNombre(String nombre, Pageable pageable);

    Optional<Rol> findByNombre(String nombre);

    // A17: búsqueda exacta case-insensitive para normalización y duplicados
    // (un rol 'administrador' no debe poder convivir con 'ADMINISTRADOR').
    Optional<Rol> findByNombreIgnoreCase(String nombre);
}
