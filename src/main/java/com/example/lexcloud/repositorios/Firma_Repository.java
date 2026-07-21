package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Firma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Firma_Repository extends JpaRepository<Firma, Integer> {
    List<Firma> findAllByNombre(String nombre);
    List<Firma> findByNombre(String nombre);
    Optional<Firma> findByNit(String nit);
}
