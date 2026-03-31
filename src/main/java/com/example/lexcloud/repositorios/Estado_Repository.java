package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Estado_Repository extends JpaRepository<Estado, Integer> {
    List<Estado> findAllByNombre(String nombre);
    Optional<Estado> findByNombre(String nombre);
}
