package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Persona;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Persona_Repository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByCc(String cc);
    // Consulta para validar ID y que el campo activo sea true
    Optional<Persona> findByIdAndActivoTrue(Integer id);

    boolean existsByCc(String cc);

    // V5: personas de una firma por el vínculo directo (personas.id_firma).
    List<Persona> findByIdFirma(Integer idFirma, Pageable pageable);

    // V5: personas de una firma por firma EFECTIVA: vínculo directo
    // (personas.id_firma) o vía su registro de abogado activo. Cubre a las
    // personas creadas antes de la V5, cuyo id_firma aún es NULL pero que
    // ya pertenecían a una firma como abogadas. H7: ORDER BY p.id.
    @Query("SELECT p FROM Persona p WHERE p.idFirma = :firmaId " +
            "OR EXISTS (SELECT 1 FROM Abogado a WHERE a.persona.id = p.id " +
            "AND a.firma.id = :firmaId AND a.activo = true) " +
            "ORDER BY p.id")
    List<Persona> findByFirmaEfectiva(@Param("firmaId") Integer firmaId, Pageable pageable);

    // V5: administradores de una firma (GET /api/firmas/{id}/administradores):
    // personas con rol ADMINISTRADOR cuya firma es personas.id_firma = id
    // O la de su registro de abogado. DISTINCT: una persona admin-abogada
    // aparece por ambas vías y no debe duplicarse. Se precargan rol y login
    // (el DTO expone nombreRol y correo). H7: ORDER BY p.id.
    @Query("SELECT DISTINCT p FROM Persona p " +
            "LEFT JOIN FETCH p.rol LEFT JOIN FETCH p.login LEFT JOIN p.abogado a " +
            "WHERE p.rol.nombre = 'ADMINISTRADOR' " +
            "AND (p.idFirma = :firmaId OR a.firma.id = :firmaId) " +
            "ORDER BY p.id")
    List<Persona> findAdministradoresPorFirma(@Param("firmaId") Integer firmaId, Pageable pageable);
}
