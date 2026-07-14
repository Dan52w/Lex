package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Discapacidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Discapacidad_Repository extends JpaRepository<Discapacidad, Integer> {
    List<Discapacidad> findAllByNombre(String nombre);
    Optional<Discapacidad> findByNombre(String nombre);
}
