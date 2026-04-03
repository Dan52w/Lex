package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Abogado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Abogado_Repository extends JpaRepository<Abogado, Integer> {

    // 1. Filtrar por Firma y Especialidad (con IgnoreCase manual)
    @Query("SELECT a FROM Abogado a WHERE a.persona.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(a.tipo_Abogado.nombre) = LOWER(:especialidad)")
    List<Abogado> findByFirmaIdAndTipoAbogadoNombreIgnoreCase(@Param("firmaId") Integer firmaId,
                                                              @Param("especialidad") String especialidad);

    // 2. Buscar un abogado específico por CC (Solo si está activo)
    @Query("SELECT a FROM Abogado a WHERE a.persona.cc = :cc AND a.persona.activo = true AND a.firma = :firmaId")
    Optional<Abogado> findByAbogadoActivobyCc(@Param("cc") String cc, @Param("firmaId") Integer firmaId);

    // 3. Buscar por nombre completo dentro de una firma (Optimizado con JOIN FETCH)
    @Query("SELECT a FROM Abogado a " +
            "JOIN FETCH a.persona " +
            "WHERE a.persona.activo = true " +
            "AND a.firma.id = :firmaId " +
            "AND LOWER(CONCAT(a.persona.nombre, ' ', a.persona.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Abogado> findByAbogadoByNombre(@Param("firmaId") Integer firmaId, @Param("nombre") String nombre);

    // 4. Listar todos los abogados activos de una firma
    @Query("SELECT a FROM Abogado a WHERE a.firma.id = :firmaId AND a.persona.activo = true")
    List<Abogado> findByAbogadosByFirmaActivos(@Param("firmaId") Integer firmaId);
}