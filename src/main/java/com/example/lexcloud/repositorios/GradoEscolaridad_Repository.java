package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Grado_Escolaridad;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradoEscolaridad_Repository extends JpaRepository<Grado_Escolaridad, Integer> {
    List<Grado_Escolaridad> findAllByNombre(String nombre);
    Optional<Grado_Escolaridad> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Grado_Escolaridad> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Grado_Escolaridad e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Grado_Escolaridad> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Grado_Escolaridad e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Grado_Escolaridad> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
