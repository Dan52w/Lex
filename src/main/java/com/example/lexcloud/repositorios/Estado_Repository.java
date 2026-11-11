package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Estado_Repository extends JpaRepository<Estado, Integer> {
    List<Estado> findAllByNombre(String nombre);
    Optional<Estado> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Estado> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Estado e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estado> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Estado e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estado> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
