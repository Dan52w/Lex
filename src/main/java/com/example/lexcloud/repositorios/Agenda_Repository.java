package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Agenda_Repository extends JpaRepository<Agenda, Integer> {

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId")
    List<Agenda> findByAbogadoId(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId")
    List<Agenda> findByCasoId(@Param("casoId") Integer casoId);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findAgendaByAbogadoAndFechaRange(
            @Param("abogadoId") Integer abogadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findByCasoIdAndFechaRange(
            @Param("casoId") Integer casoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "AND a.fechaExp > CURRENT_TIMESTAMP " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findProximasCitasByAbogado(@Param("abogadoId") Integer abogadoId);
}
}