package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCaso_Repository extends JpaRepository<Tipo_Caso, Integer> {
    List<Tipo_Caso> findAllByNombre(String nombre);
    Optional<Tipo_Caso> findByNombre(String nombre);
}
