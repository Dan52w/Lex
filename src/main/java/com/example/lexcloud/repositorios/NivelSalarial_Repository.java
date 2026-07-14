package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Nivel_Salarial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NivelSalarial_Repository extends JpaRepository<Nivel_Salarial, Integer> {
    List<Nivel_Salarial> findAllByRango(String rango);
    Optional<Nivel_Salarial> findByRango(String rango);
}
