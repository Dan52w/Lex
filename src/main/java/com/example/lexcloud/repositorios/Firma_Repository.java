package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Firma;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Firma_Repository extends JpaRepository<Firma, Integer> {
    List<Firma> findAllByNombre(String nombre);

    // M1: variante paginada del listado por nombre (mismo criterio exacto).
    List<Firma> findAllByNombre(String nombre, Pageable pageable);

    // H10: se borró findByNombre (List, duplicado exacto de findAllByNombre
    // sin llamadores). Convenio del resto de catálogos: List = búsqueda,
    // Optional = exacto por nombre único.
    Optional<Firma> findByNit(String nit);
}
