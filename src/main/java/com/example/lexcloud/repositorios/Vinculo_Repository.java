package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Vinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Vinculo_Repository extends JpaRepository<Vinculo, Integer> {
    List<Vinculo> findAllByNombre(String nombre);
    Optional<Vinculo> findByNombre(String nombre);
}
