package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Persona_Repository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByCcAndActivo(String cc, Boolean activo);
}
