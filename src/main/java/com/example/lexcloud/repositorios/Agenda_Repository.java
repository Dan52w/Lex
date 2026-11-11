package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Agenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Agenda_Repository extends JpaRepository<Agenda, Integer> {

    // H2: todas las consultas de listado filtran a.activa = true (verificado:
    // ningún llamador en AgendaServiceImpl necesita las inactivas — son
    // listados normales; el historial/admin no las usa). Antes solo
    // findProximasCitasByAbogado lo hacía.
    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true")
    List<Agenda> findByAbogadoId(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId " +
            "AND a.activa = true")
    List<Agenda> findByCasoId(@Param("casoId") Integer casoId);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC, a.id ASC")
    List<Agenda> findAgendaByAbogadoAndFechaRange(
            @Param("abogadoId") Integer abogadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId " +
            "AND a.activa = true " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC, a.id ASC")
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
            "ORDER BY a.fechaExp ASC, a.id ASC")
    List<Agenda> findProximasCitasByAbogado(@Param("abogadoId") Integer abogadoId);

    // =========================================================================
    // VARIANTES PAGINADAS
    // Misma consulta que arriba, con Pageable para no traer listas sin límite
    // (las citas de un abogado se acumulan con el tiempo). Los servicios deben
    // migrar los listados a estas variantes.
    // H7: cada variante paginada lleva ORDER BY con el id como desempate
    // (o como orden único cuando no hay otro criterio).
    // =========================================================================

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "ORDER BY a.id")
    List<Agenda> findByAbogadoId(@Param("abogadoId") Integer abogadoId, Pageable pageable);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId " +
            "AND a.activa = true " +
            "ORDER BY a.id")
    List<Agenda> findByCasoId(@Param("casoId") Integer casoId, Pageable pageable);

    // H6: variantes paginadas que faltaban (las usa AgendaServiceImpl:112,123
    // en su forma sin paginar). Devuelven List, no Page, para no romper DTOs.
    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC, a.id ASC")
    List<Agenda> findAgendaByAbogadoAndFechaRange(
            @Param("abogadoId") Integer abogadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    @Query("SELECT a FROM Agenda a " +
            "JOIN FETCH a.tipo_Agenda ta " +
            "JOIN FETCH a.caso c " +
            "WHERE a.caso.id = :casoId " +
            "AND a.activa = true " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC, a.id ASC")
    List<Agenda> findByCasoIdAndFechaRange(
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
            "ORDER BY a.fechaExp ASC, a.id ASC")
    List<Agenda> findProximasCitasByAbogado(@Param("abogadoId") Integer abogadoId, Pageable pageable);

    /**
     * A7 (lote 7): ¿existe otra agenda ACTIVA del mismo abogado en la misma
     * fecha/hora? Derivado sobre los campos de Agenda (abogado, fechaExp,
     * activa). La variante con IdNot excluye la propia agenda en actualizar().
     */
    boolean existsByAbogado_IdAndFechaExpAndActivaTrue(
            Integer abogadoId, LocalDateTime fechaExp);

    boolean existsByAbogado_IdAndFechaExpAndActivaTrueAndIdNot(
            Integer abogadoId, LocalDateTime fechaExp, Integer id);
}
