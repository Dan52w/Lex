package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface Agenda_Repository extends JpaRepository<Agenda, Integer> {

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId")
    List<Agenda> findByAbogadoId(@Param("abogadoId") Integer abogadoId);

    Page<Agenda> findByAbogadoId(@Param("abogadoId") Integer abogadoId, Pageable pageable);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId")
    List<Agenda> findByCasoId(@Param("casoId") Integer casoId);

    Page<Agenda> findByCasoId(@Param("casoId") Integer casoId, Pageable pageable);

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

    // P1-B2: sin FETCH (con Pageable, FETCH paginaría en memoria).
    @Query("SELECT a FROM Agenda a " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    Page<Agenda> findAgendaByAbogadoAndFechaRange(
            @Param("abogadoId") Integer abogadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

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

    // P1-B2: sin FETCH, igual que arriba.
    @Query("SELECT a FROM Agenda a " +
            "WHERE a.caso.id = :casoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    Page<Agenda> findByCasoIdAndFechaRange(
            @Param("casoId") Integer casoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "AND a.fechaExp > CURRENT_TIMESTAMP " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findProximasCitasByAbogado(@Param("abogadoId") Integer abogadoId);
}