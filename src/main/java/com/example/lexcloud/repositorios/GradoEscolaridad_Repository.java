package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Grado_Escolaridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradoEscolaridad_Repository extends JpaRepository<Grado_Escolaridad, Integer> {
    List<Grado_Escolaridad> findAllByNombre(String nombre);
    Optional<Grado_Escolaridad> findByNombre(String nombre);
}
