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

    // 1. Encontrar por CC y Firma (Corregido)
    @Query("SELECT c FROM Cliente c WHERE c.persona.cc = :cc AND c.persona.activo = true AND c.firma.id = :firmaId")
    Optional<Cliente> findByCcAndFirma(@Param("cc") String cc, @Param("firmaId") Integer firmaId);

    // 2. Encontrar por ID de Persona (No el ID del cliente, sino de la entidad Persona)
    @Query("SELECT c FROM Cliente c WHERE c.persona.id = :personaId AND c.persona.activo = true")
    Optional<Cliente> findByPersonaId(@Param("personaId") Integer personaId);

    // 3. Listar TODOS los clientes de una firma específica
    @Query("SELECT c FROM Cliente c JOIN FETCH c.persona WHERE c.firma.id = :firmaId AND c.persona.activo = true")
    List<Cliente> findAllByFirmaId(@Param("firmaId") Integer firmaId);

    // 4. Buscar clientes por nombre dentro de una firma
    @Query("SELECT c FROM Cliente c JOIN FETCH c.persona " +
            "WHERE c.firma.id = :firmaId " +
            "AND c.persona.activo = true " +
            "AND LOWER(CONCAT(c.persona.nombre, ' ', c.persona.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Cliente> findByNombreAndFirma(@Param("firmaId") Integer firmaId, @Param("nombre") String nombre);
}

