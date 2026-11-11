package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Discapacidad;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Discapacidad_Repository extends JpaRepository<Discapacidad, Integer> {
    List<Discapacidad> findAllByNombre(String nombre);
    Optional<Discapacidad> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Discapacidad> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Discapacidad e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Discapacidad> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Discapacidad e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Discapacidad> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
