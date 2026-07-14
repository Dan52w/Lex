package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Identificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoIdentificacion_Repository extends JpaRepository<Tipo_Identificacion, Integer> {
    List<Tipo_Identificacion> findAllByNombre(String nombre);
    Optional<Tipo_Identificacion> findByNombre(String nombre);
}
