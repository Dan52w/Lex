package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estado_Civil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoCivil_Repository extends JpaRepository<Estado_Civil, Integer> {
    List<Estado_Civil> findAllByNombre(String nombre);
    Optional<Estado_Civil> findByNombre(String nombre);
}
