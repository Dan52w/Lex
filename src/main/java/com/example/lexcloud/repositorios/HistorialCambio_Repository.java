package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Historial_Cambio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialCambio_Repository extends JpaRepository<Historial_Cambio, Integer> {
    // H14: fecha es DATE (LocalDate), así que varios cambios del mismo día
    // empataban; h.id DESC desempata (el id mayor es el cambio más reciente).
    @Query("SELECT h FROM Historial_Cambio h " +
            "JOIN FETCH h.caso c " +
            "JOIN FETCH h.persona p " +
            "WHERE h.caso.id = :casoId " +
            "ORDER BY h.fecha DESC, h.id DESC")
    List<Historial_Cambio> findByCasoId(@Param("casoId") Integer casoId);

    // Variante paginada: el historial de un caso crece con cada cambio.
    @Query("SELECT h FROM Historial_Cambio h " +
            "JOIN FETCH h.caso c " +
            "JOIN FETCH h.persona p " +
            "WHERE h.caso.id = :casoId " +
            "ORDER BY h.fecha DESC, h.id DESC")
    List<Historial_Cambio> findByCasoId(@Param("casoId") Integer casoId, Pageable pageable);
}
