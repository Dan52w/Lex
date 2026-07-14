package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Vivienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoVivienda_Repository extends JpaRepository<Tipo_Vivienda, Integer> {
    List<Tipo_Vivienda> findAllByNombre(String nombre);
    Optional<Tipo_Vivienda> findByNombre(String nombre);
}
