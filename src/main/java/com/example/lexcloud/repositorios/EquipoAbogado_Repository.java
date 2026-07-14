package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Equipo_Abogados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoAbogado_Repository extends JpaRepository<Equipo_Abogados, Integer> {

    // 1. Buscar todas las asignaciones de un abogado en específico
    List<Equipo_Abogados> findByAbogado_Id(Integer idAbogado);

    // 2. Buscar las asignaciones activas de un abogado
    List<Equipo_Abogados> findByAbogado_IdAndActivoTrue(Integer idAbogado);

    // 3. Buscar quiénes han sido asignados por un miembro administrativo (Persona)
    List<Equipo_Abogados> findByPersona_Id(Integer idPersona);

    // 4. Buscar asignaciones por un tipo de relación específico (ej: "Líder", "Socio", "Asistente")
    List<Equipo_Abogados> findByTipoRelacion_Id(Integer idTipoRelacion);

    // 5. Verificar si ya existe una asignación activa específica entre un Abogado y un miembro administrativo (Persona)
    boolean existsByAbogado_IdAndPersona_IdAndActivoTrue(Integer idAbogado, Integer idPersona);

    // 6. Buscar una asignación activa concreta (útil para desactivarla o actualizarla)
    Optional<Equipo_Abogados> findByAbogado_IdAndPersona_IdAndActivoTrue(Integer idAbogado, Integer idPersona);

    // 7. Consulta personalizada (JPQL) para traer los detalles del equipo con carga perezosa resuelta (evita problema N+1)
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE ea.activo = true")
    List<Equipo_Abogados> findAllActiveWithRelationships();

    // 8. Buscar los abogados activos asignados bajo un rol/relación específico
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "WHERE ea.tipoRelacion.id = :idTipoRelacion AND ea.activo = true")
    List<Equipo_Abogados> findActiveByRelationshipType(@Param("idTipoRelacion") Integer idTipoRelacion);
}