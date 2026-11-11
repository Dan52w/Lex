package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Vivienda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoVivienda_Repository extends JpaRepository<Tipo_Vivienda, Integer> {
    List<Tipo_Vivienda> findAllByNombre(String nombre);
    Optional<Tipo_Vivienda> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Tipo_Vivienda> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Tipo_Vivienda e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Tipo_Vivienda> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Tipo_Vivienda e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Tipo_Vivienda> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
