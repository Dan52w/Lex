package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Historial_Cambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface HistorialCambio_Repository extends JpaRepository<Historial_Cambio, Integer> {
    @Query("SELECT h FROM Historial_Cambio h " +
            "JOIN FETCH h.caso c " +
            "JOIN FETCH h.persona p " +
            "WHERE h.caso.id = :casoId " +
            "ORDER BY h.fecha DESC")
    List<Historial_Cambio> findByCasoId(@Param("casoId") Integer casoId);

    // Sin FETCH: con Pageable, FETCH paginaría en memoria (HHH000104).
    @Query("SELECT h FROM Historial_Cambio h " +
            "WHERE h.caso.id = :casoId " +
            "ORDER BY h.fecha DESC")
    Page<Historial_Cambio> findByCasoId(@Param("casoId") Integer casoId, Pageable pageable);
}
