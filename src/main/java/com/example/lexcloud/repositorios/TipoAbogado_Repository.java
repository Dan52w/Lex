package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Abogado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoAbogado_Repository extends JpaRepository<Tipo_Abogado, Integer> {
    Optional<Tipo_Abogado> findAllByNombre(String nombre);
}
