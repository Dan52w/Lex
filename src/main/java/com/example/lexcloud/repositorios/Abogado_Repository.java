package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Abogado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Abogado_Repository extends JpaRepository<Abogado, Integer> {

    // 1. Filtrar por Firma y Especialidad (con IgnoreCase manual)
    // A5: además de persona.activo, exige el nuevo a.activo = true.
    @Query("SELECT a FROM Abogado a WHERE a.persona.activo = true " +
            "AND a.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(a.tipo_Abogado.nombre) = LOWER(:especialidad)")
    List<Abogado> findByFirmaIdAndTipoAbogadoNombreIgnoreCase(@Param("firmaId") Integer firmaId,
                                                              @Param("especialidad") String especialidad);

    // 2. Buscar un abogado específico por CC (Solo si está activo)
    // H13: renombrado findByAbogadoActivobyCc -> findByAbogadoActivoByCc.
    // A5: exige a.activo = true.
    @Query("SELECT a FROM Abogado a WHERE a.persona.cc = :cc AND a.persona.activo = true AND a.activo = true AND a.firma.id = :firmaId")
    Optional<Abogado> findByAbogadoActivoByCc(@Param("cc") String cc, @Param("firmaId") Integer firmaId);

    // Chequeo de duplicado: debe ver también los inactivos, sin filtro a.activo.
    Optional<Abogado> findByPersonaId(Integer personaId);

    // 3. Buscar por nombre completo dentro de una firma (Optimizado con JOIN FETCH)
    // ESCAPE '\': el nombre viene parametrizado, pero %, _ y \ dentro del nombre
    // seguirían actuando como comodines LIKE; el ESCAPE permite que el servicio
    // los neutralice escapándolos antes de llamar.
    // H13: renombrado findByAbogadoByNombre -> findByNombre (las 2 variantes).
    // A5: exige a.activo = true.
    @Query("SELECT a FROM Abogado a " +
            "JOIN FETCH a.persona " +
            "WHERE a.persona.activo = true " +
            "AND a.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(CONCAT(a.persona.nombre, ' ', a.persona.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%')) ESCAPE '\\'")
    List<Abogado> findByNombre(@Param("firmaId") Integer firmaId, @Param("nombre") String nombre);

    // 4. Listar todos los abogados activos de una firma
    // (A5 no añade a.activo = true aquí: la enumeración del lote cubre
    // findByFirmaId..., findByNombre y findByAbogadoActivoByCc; este método
    // sigue filtrando por persona.activo = true como antes.)
    @Query("SELECT a FROM Abogado a WHERE a.firma.id = :firmaId AND a.persona.activo = true AND a.activo = true")
    List<Abogado> findByAbogadosByFirmaActivos(@Param("firmaId") Integer firmaId);

    // =========================================================================
    // VARIANTES PAGINADAS
    // Misma consulta que arriba, con Pageable para no traer listas sin límite.
    // Los servicios deben migrar los listados a estas variantes.
    // H7: cada variante paginada lleva ORDER BY a.id.
    // =========================================================================

    // H6: variante paginada que faltaba (la usa AbogadoServiceImpl:104 en su
    // forma sin paginar). Devuelve List, no Page, para no romper DTOs.
    @Query("SELECT a FROM Abogado a WHERE a.persona.activo = true " +
            "AND a.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(a.tipo_Abogado.nombre) = LOWER(:especialidad) " +
            "ORDER BY a.id")
    List<Abogado> findByFirmaIdAndTipoAbogadoNombreIgnoreCase(@Param("firmaId") Integer firmaId,
                                                              @Param("especialidad") String especialidad,
                                                              Pageable pageable);

    @Query("SELECT a FROM Abogado a " +
            "JOIN FETCH a.persona " +
            "WHERE a.persona.activo = true " +
            "AND a.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(CONCAT(a.persona.nombre, ' ', a.persona.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%')) ESCAPE '\\' " +
            "ORDER BY a.id")
    List<Abogado> findByNombre(@Param("firmaId") Integer firmaId, @Param("nombre") String nombre, Pageable pageable);

    @Query("SELECT a FROM Abogado a WHERE a.firma.id = :firmaId AND a.persona.activo = true AND a.activo = true " +
            "ORDER BY a.id")
    List<Abogado> findByAbogadosByFirmaActivos(@Param("firmaId") Integer firmaId, Pageable pageable);
}
