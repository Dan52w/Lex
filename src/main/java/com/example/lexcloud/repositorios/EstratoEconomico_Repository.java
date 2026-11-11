package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Estrato_Economico;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstratoEconomico_Repository extends JpaRepository<Estrato_Economico, Integer> {
    List<Estrato_Economico> findAllByNombre(String nombre);
    Optional<Estrato_Economico> findByNombre(String nombre);

    /** A17 — duplicado case-insensitive por nombre normalizado. */
    Optional<Estrato_Economico> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Estrato_Economico e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estrato_Economico> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Estrato_Economico e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Estrato_Economico> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);
}
