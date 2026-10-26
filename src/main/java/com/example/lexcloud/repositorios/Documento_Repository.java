package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface Documento_Repository extends JpaRepository<Documento, Integer> {
    @Query("SELECT d FROM Documento d " +
            "JOIN FETCH d.caso c " +
            "JOIN FETCH d.tipo_Documento td " +
            "WHERE d.caso.id = :casoId")
    List<Documento> findByCasoId(@Param("casoId") Integer casoId);

    // P1-B2/B7: solo documentos no enviados a la papelera.
    // Sin FETCH: con Pageable, FETCH paginaría en memoria (HHH000104).
    @Query("SELECT d FROM Documento d " +
            "WHERE d.caso.id = :casoId AND d.eliminadoEn IS NULL")
    Page<Documento> findActivosByCasoId(@Param("casoId") Integer casoId, Pageable pageable);

    @Query("SELECT d FROM Documento d " +
            "WHERE d.eliminadoEn IS NOT NULL AND d.caso.firma.id = :firmaId " +
            "ORDER BY d.eliminadoEn DESC")
    Page<Documento> findPapeleraByFirmaId(@Param("firmaId") Integer firmaId, Pageable pageable);
}
