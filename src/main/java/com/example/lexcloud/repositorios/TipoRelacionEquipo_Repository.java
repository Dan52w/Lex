package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Relacion_Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoRelacionEquipo_Repository extends JpaRepository<Tipo_Relacion_Equipo, Integer> {
    List<Tipo_Relacion_Equipo> findAllByNombre(String nombre);
    Optional<Tipo_Relacion_Equipo> findByNombre(String nombre);
}
