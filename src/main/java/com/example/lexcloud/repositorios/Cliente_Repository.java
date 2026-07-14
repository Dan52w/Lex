package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // 3. EL SUPER BUSCADOR
    // Busca coincidencias parciales (case-insensitive) en: Identificación, Nombre Completo, Correo o Teléfono Móvil.
    // Siempre restringido a la Firma del abogado y que el cliente esté activo.
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.tipoIdentificacion " +
            "LEFT JOIN FETCH c.tipoCliente " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.activo = true " +
            "AND (" +
            "  LOWER(c.identificacion) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "  LOWER(CONCAT(c.nombres, ' ', c.primerApellido, ' ', COALESCE(c.segundoApellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "  LOWER(c.correo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "  c.telefonoMovil LIKE CONCAT('%', :filtro, '%')" +
            ")")
    List<Cliente> superBuscador(
            @Param("filtro") String filtro,
            @Param("firmaId") Integer firmaId
    );
}