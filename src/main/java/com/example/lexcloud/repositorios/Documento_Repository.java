package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Documento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Documento_Repository extends JpaRepository<Documento, Integer> {
    @Query("SELECT d FROM Documento d " +
            "JOIN FETCH d.caso c " +
            "JOIN FETCH d.tipo_Documento td " +
            "WHERE d.caso.id = :casoId")
    List<Documento> findByCasoId(@Param("casoId") Integer casoId);

    // Variante paginada: un caso puede acumular muchos documentos.
    // H7: ORDER BY d.id para un orden determinista entre páginas.
    @Query("SELECT d FROM Documento d " +
            "JOIN FETCH d.caso c " +
            "JOIN FETCH d.tipo_Documento td " +
            "WHERE d.caso.id = :casoId " +
            "ORDER BY d.id")
    List<Documento> findByCasoId(@Param("casoId") Integer casoId, Pageable pageable);
}
