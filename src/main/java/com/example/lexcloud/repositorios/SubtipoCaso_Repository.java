package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Subtipo_Caso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtipoCaso_Repository extends JpaRepository<Subtipo_Caso, Integer> {
    List<Subtipo_Caso> findAllByNombre(String nombre);

    /**
     * H1: valida la unicidad por (tipo de caso, nombre) — V1 define
     * UNIQUE(id_tipo_caso, nombre) compuesta, así que el nombre solo es
     * único dentro de su tipo. El anterior findByNombre(String) global
     * devolvía Optional y reventaba con 500 si dos tipos compartían nombre.
     */
    Optional<Subtipo_Caso> findByTipoCasoIdAndNombre(Integer tipoCasoId, String nombre);

    List<Subtipo_Caso> findByTipoCasoId(Integer idTipoCaso);

    /** A17 — duplicado case-insensitive (solo uniformidad; el duplicado real de subtipos es por (tipoCaso, nombre)). */
    Optional<Subtipo_Caso> findByNombreIgnoreCase(String nombre);

    /**
     * M15 — búsqueda PARCIAL case-insensitive. El término llega ya escapado
     * con BusquedaUtil.escaparLike; ESCAPE '\\' neutraliza %, _ y \\.
     */
    @Query("SELECT e FROM Subtipo_Caso e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Subtipo_Caso> buscarPorNombreParcial(@Param("termino") String termino);

    @Query("SELECT e FROM Subtipo_Caso e WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\'")
    List<Subtipo_Caso> buscarPorNombreParcial(@Param("termino") String termino, Pageable pageable);

    /** M1 — variante paginada de findByTipoCasoId para listarPorTipo(page, size). */
    List<Subtipo_Caso> findByTipoCasoId(Integer idTipoCaso, Pageable pageable);
}
