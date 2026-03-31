package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoDocumento_Repository extends JpaRepository<Tipo_Documento, Integer> {
    List<Tipo_Documento> findAllByNombre(String nombre);
    Optional<Tipo_Documento> findByNombre(String nombre);
}
