package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Nivel_Salarial;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NivelSalarial_Repository extends JpaRepository<Nivel_Salarial, Integer> {
    List<Nivel_Salarial> findAllByRango(String rango);
    Optional<Nivel_Salarial> findByRango(String rango);

    /** A17 — duplicado case-insensitive por rango normalizado. */
    Optional<Nivel_Salarial> findByRangoIgnoreCase(String rango);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Nivel_Salarial e WHERE LOWER(e.rango) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Nivel_Salarial> buscarPorRangoParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Nivel_Salarial e WHERE LOWER(e.rango) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Nivel_Salarial> buscarPorRangoParcial(@Param("termino") String termino, Pageable pageable);
}
