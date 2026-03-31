package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Subtipo_Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtipoCaso_Repository extends JpaRepository<Subtipo_Caso, Integer> {
    List<Subtipo_Caso> findAllByNombre(String nombre);
    Optional<Subtipo_Caso> findByNombre(String nombre);
}
