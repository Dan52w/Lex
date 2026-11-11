package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estado_Civil;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoCivil_Repository extends JpaRepository<Estado_Civil, Integer> {
    List<Estado_Civil> findAllByNombre(String nombre);
    Optional<Estado_Civil> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Estado_Civil> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Estado_Civil e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estado_Civil> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Estado_Civil e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estado_Civil> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
