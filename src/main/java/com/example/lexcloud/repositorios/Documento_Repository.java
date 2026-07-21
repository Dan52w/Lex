package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Documento;
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
}
