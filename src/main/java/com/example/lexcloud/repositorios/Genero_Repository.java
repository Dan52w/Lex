package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Genero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Genero_Repository extends JpaRepository<Genero, Integer> {
    List<Genero> findAllByNombre(String nombre);
    Optional<Genero> findByNombre(String nombre);
}
