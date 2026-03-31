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
    // 1. Todas las agendas de un abogado específico
    List<Agenda> findByAbogadoId(Integer abogadoId);

    // 2. BUSCAR AGENDA POR CASO
    // Esto permite ver el historial de citas o eventos de un expediente concreto
    List<Agenda> findByCasoId(Integer casoId);

    // 3. Filtrar por rango de fechas para un abogado (Corregido a fechaExp según tu DB)
    @Query("SELECT a FROM Agenda a WHERE a.abogado.id = :abogadoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findAgendaByAbogadoAndFechaRange(
            @Param("abogadoId") Integer abogadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // 4. Buscar agendas de un CASO en un rango de fechas
    // Útil para saber qué viene esta semana para un proceso específico
    @Query("SELECT a FROM Agenda a WHERE a.caso.id = :casoId " +
            "AND a.fechaExp >= :inicio AND a.fechaExp <= :fin " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findByCasoIdAndFechaRange(
            @Param("casoId") Integer casoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // 5. Buscar citas próximas del abogado (activas y futuras)
    @Query("SELECT a FROM Agenda a WHERE a.abogado.id = :abogadoId " +
            "AND a.activa = true " +
            "AND a.fechaExp > CURRENT_TIMESTAMP " +
            "ORDER BY a.fechaExp ASC")
    List<Agenda> findProximasCitasByAbogado(@Param("abogadoId") Integer abogadoId);
}