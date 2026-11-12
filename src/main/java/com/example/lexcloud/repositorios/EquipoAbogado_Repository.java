package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Equipo_Abogados;
import org.springframework.data.domain.Pageable;
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

    // H3: variante paginada de (1) — devuelve List, no Page, para no romper DTOs.
    // El orden lo fija Paginacion (Sort.by("id") por defecto, H7).
    List<Equipo_Abogados> findByAbogado_Id(Integer idAbogado, Pageable pageable);

    // 2. Buscar las asignaciones activas de un abogado
    List<Equipo_Abogados> findByAbogado_IdAndActivoTrue(Integer idAbogado);

    // M1 (Lote 9, corrección del coordinador): variante paginada de (2) para
    // que listarActivosPorAbogado no filtre activo en memoria (una página
    // podía salir corta). Mismo patrón que la variante paginada de (1).
    List<Equipo_Abogados> findByAbogado_IdAndActivoTrue(Integer idAbogado, Pageable pageable);

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

    // 8. H4: se borró findActiveByRelationshipType (global, sin firma; su único
    // llamador era la rama admin de EquipoAbogadoServiceImpl:161, que el
    // lote de servicios migrará a la variante con firma y paginada).
    // La variante con firma sin paginar se conserva para esa migración.

    // =========================================================================
    // VARIANTES CON ALCANCE POR FIRMA Y PAGINADAS
    // Los métodos 7 y 8 no filtran por firma: devuelven asignaciones de TODAS
    // las firmas, así que el servicio que los use debe ser solo-admin o pasar
    // a estas variantes con firmaId. La variante paginada evita traer listas
    // sin límite cuando la firma crece.
    // H7: cada variante paginada lleva ORDER BY ea.id.
    // =========================================================================

    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE ea.activo = true AND a.firma.id = :firmaId")
    List<Equipo_Abogados> findActiveWithRelationshipsByFirma(@Param("firmaId") Integer firmaId);

    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE ea.activo = true AND a.firma.id = :firmaId " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findActiveWithRelationshipsByFirma(@Param("firmaId") Integer firmaId, Pageable pageable);

    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE ea.activo = true " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findAllActiveWithRelationships(Pageable pageable);

    // H3/C8: mismo listado que findActiveWithRelationshipsByFirma pero con el
    // nombre que el lote de servicios (C8) espera; lo necesita otro lote.
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE ea.activo = true AND a.firma.id = :firmaId " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findAllActiveWithRelationshipsByFirma(@Param("firmaId") Integer firmaId, Pageable pageable);

    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "WHERE ea.tipoRelacion.id = :idTipoRelacion AND ea.activo = true AND a.firma.id = :firmaId")
    List<Equipo_Abogados> findActiveByRelationshipTypeAndFirma(
            @Param("idTipoRelacion") Integer idTipoRelacion,
            @Param("firmaId") Integer firmaId);

    // H4: variante con firma Y paginada de la anterior.
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "WHERE ea.tipoRelacion.id = :idTipoRelacion AND ea.activo = true AND a.firma.id = :firmaId " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findActiveByRelationshipTypeAndFirma(
            @Param("idTipoRelacion") Integer idTipoRelacion,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    // H3: variantes con alcance por firma y paginación de (3) y (4).
    // Misma semántica que los derivados (sin filtro de activo — el servicio
    // ya filtra en memoria lo que necesita), más firma y Pageable.
    // Son @Query porque Equipo_Abogados no tiene propiedad directa `firma`
    // (la firma cuelga de ea.abogado.firma).
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE p.id = :idPersona AND a.firma.id = :firmaId " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findByPersonaIdAndFirma(
            @Param("idPersona") Integer idPersona,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE tr.id = :idTipoRelacion AND a.firma.id = :firmaId " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findByTipoRelacionIdAndFirma(
            @Param("idTipoRelacion") Integer idTipoRelacion,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    // =========================================================================
    // VARIANTES GLOBALES (TODAS LAS FIRMAS), PAGINADAS
    // Solo las llama el servicio para el súper admin y el admin bootstrap
    // (sin firma resoluble): un admin CON firma nunca cae aquí (el
    // servicio lo desvía a la variante con firma) y un no-admin sin firma
    // recibe 403 en el helper del servicio antes de llegar al repositorio.
    // =========================================================================

    // Global de findByPersonaIdAndFirma (misma semántica: sin filtro de
    // activo, el servicio ya filtra en memoria lo que necesita).
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE p.id = :idPersona " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findByPersonaId(
            @Param("idPersona") Integer idPersona,
            Pageable pageable);

    // Global de findByTipoRelacionIdAndFirma.
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "JOIN FETCH ea.persona p " +
            "JOIN FETCH ea.tipoRelacion tr " +
            "WHERE tr.id = :idTipoRelacion " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findByTipoRelacionId(
            @Param("idTipoRelacion") Integer idTipoRelacion,
            Pageable pageable);

    // Global de findActiveByRelationshipTypeAndFirma (la variante global
    // sin paginar se borró en H4; esta es su equivalente paginada).
    @Query("SELECT ea FROM Equipo_Abogados ea " +
            "JOIN FETCH ea.abogado a " +
            "WHERE ea.tipoRelacion.id = :idTipoRelacion AND ea.activo = true " +
            "ORDER BY ea.id")
    List<Equipo_Abogados> findActiveByRelationshipType(
            @Param("idTipoRelacion") Integer idTipoRelacion,
            Pageable pageable);
}
