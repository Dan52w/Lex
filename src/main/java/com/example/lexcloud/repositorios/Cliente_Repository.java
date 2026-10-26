package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface Cliente_Repository extends JpaRepository<Cliente, Integer> {

    // 1. Encontrar un cliente específico por su Identificación dentro de la Firma del abogado
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.tipoIdentificacion " +
            "LEFT JOIN FETCH c.tipoCliente " +
            "WHERE c.identificacion = :identificacion " +
            "AND c.activo = true " +
            "AND c.firma.id = :firmaId")
    Optional<Cliente> findByIdentificacionAndFirma(
            @Param("identificacion") String identificacion,
            @Param("firmaId") Integer firmaId
    );

    // 2. Listar TODOS los clientes activos pertenecientes a la Firma del abogado
    // (Usamos FETCH para traer las relaciones más comunes de golpe y evitar el problema de consultas N+1)
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.tipoIdentificacion " +
            "LEFT JOIN FETCH c.tipoCliente " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.activo = true")
    List<Cliente> findAllByFirmaId(@Param("firmaId") Integer firmaId);

    // Sin FETCH a propósito: con Pageable, un FETCH haría que Hibernate
    // trajera todas las filas y paginara en memoria (HHH000104).
    @Query("SELECT c FROM Cliente c " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.activo = true")
    Page<Cliente> findAllByFirmaId(@Param("firmaId") Integer firmaId, Pageable pageable);

    // 3. EL SUPER BUSCADOR
    // Busca coincidencias parciales (case-insensitive) en: Identificación, Nombre Completo, Correo o Teléfono Móvil.
    // Siempre restringido a la Firma del abogado y que el cliente esté activo.
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.tipoIdentificacion " +
            "LEFT JOIN FETCH c.tipoCliente " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.activo = true " +
            "AND (" +
            "  LOWER(c.identificacion) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  LOWER(CONCAT(c.nombres, ' ', c.primerApellido, ' ', COALESCE(c.segundoApellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  LOWER(c.correo) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  c.telefonoMovil LIKE CONCAT('%', :filtro, '%') ESCAPE '\\'" +
            ")")
    List<Cliente> superBuscador(
            @Param("filtro") String filtro,
            @Param("firmaId") Integer firmaId
    );

    // P1-B2: misma búsqueda, paginada, sin FETCH por la misma razón.
    @Query("SELECT c FROM Cliente c " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.activo = true " +
            "AND (" +
            "  LOWER(c.identificacion) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  LOWER(CONCAT(c.nombres, ' ', c.primerApellido, ' ', COALESCE(c.segundoApellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  LOWER(c.correo) LIKE LOWER(CONCAT('%', :filtro, '%')) ESCAPE '\\' OR " +
            "  c.telefonoMovil LIKE CONCAT('%', :filtro, '%') ESCAPE '\\'" +
            ")")
    Page<Cliente> superBuscador(
            @Param("filtro") String filtro,
            @Param("firmaId") Integer firmaId,
            Pageable pageable
    );
}