package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estrato_Economico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstratoEconomico_Repository extends JpaRepository<Estrato_Economico, Integer> {
    List<Estrato_Economico> findAllByNombre(String nombre);
    Optional<Estrato_Economico> findByNombre(String nombre);
}
