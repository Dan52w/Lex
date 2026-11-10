package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Vinculo_Caso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VinculoCaso_Repository extends JpaRepository<Vinculo_Caso, Integer> {

    // 1. Verificar si un abogado/personal tiene acceso autorizado a un caso
    // Corregido a PersonaFirma_Id y Caso_Id para evitar fallos de mapeo en Spring Data
    boolean existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(Integer personaId, Integer casoId);

    // 2. Verificar si un cliente específico está vinculado a un caso
    // CORREGIDO: Ahora busca por Cliente_Id de la nueva entidad Cliente
    boolean existsByCliente_IdAndCaso_IdAndAutorizadoTrue(Integer clienteId, Integer casoId);

    // 2b. Verificar si ya existe un vínculo para la misma persona, cliente y caso
    // (la BD tiene UNIQUE sobre esa terna; el servicio lo comprueba antes para
    // devolver 409 con mensaje claro en vez de la violación genérica).
    boolean existsByPersonaFirma_IdAndCliente_IdAndCaso_Id(Integer personaId, Integer clienteId, Integer casoId);

    // 3. Listar todos los abogados/personal asignados a un caso (Optimizado con FETCH)
    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma p " +
            "JOIN FETCH vc.vinculo v " +
            "WHERE vc.caso.id = :casoId AND vc.autorizado = true")
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(@Param("casoId") Integer casoId);

    // 4. Listar todos los casos donde un abogado/personal está vinculado ("Mis Casos" para el abogado)
    List<Vinculo_Caso> findByPersonaFirma_IdAndAutorizadoTrue(Integer personaId);

    // =========================================================================
    // Métodos Administrativos y de Control
    // =========================================================================

    // 8. Buscar quién es el abogado principal de un caso
    @Query("SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = 'Principal' AND vc.autorizado = true")
    Optional<Vinculo_Caso> findAbogadoPrincipalDelCaso(@Param("casoId") Integer casoId);

    // 8b. Principales de un caso como lista: si por datos viejos hubiera dos
    // vínculos 'Principal', el Optional de arriba daría 500; esta variante
    // permite tomar el primero sin romper (la guarda SVC-9 en el servicio
    // impide que se creen dos 'Principal' por caso a partir de ahora).
    @Query("SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = 'Principal' AND vc.autorizado = true")
    List<Vinculo_Caso> findPrincipalesDelCaso(@Param("casoId") Integer casoId);

    // 9. Todos los vínculos de un caso (el 'Principal' primero). Reemplaza al
    // antiguo findByCaso_Id, que devolvía Optional y rompía con 500 cuando un
    // caso tenía N vínculos (SVC-1): el servicio toma el primero.
    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma " +
            "JOIN FETCH vc.vinculo " +
            "JOIN FETCH vc.cliente " +
            "JOIN FETCH vc.caso " +
            "WHERE vc.caso.id = :casoId " +
            "ORDER BY CASE WHEN vc.vinculo.nombre = 'Principal' THEN 0 ELSE 1 END")
    List<Vinculo_Caso> findVinculosDelCaso(@Param("casoId") Integer casoId);

    // 10. Listar vínculos pendientes de autorización (Para panel de administración)
    List<Vinculo_Caso> findByAutorizadoFalse();

    // =========================================================================
    // VARIANTES PAGINADAS Y CON ALCANCE POR FIRMA
    // findByAutorizadoFalse (10) no filtra por firma: devuelve pendientes de
    // TODAS las firmas, así que solo lo debe usar un admin. La variante con
    // firmaId es la que deben usar los servicios por firma. Las variantes
    // paginadas evitan traer listas sin límite.
    // =========================================================================

    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma p " +
            "JOIN FETCH vc.vinculo v " +
            "WHERE vc.caso.id = :casoId AND vc.autorizado = true")
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(@Param("casoId") Integer casoId, Pageable pageable);

    List<Vinculo_Caso> findByPersonaFirma_IdAndAutorizadoTrue(Integer personaId, Pageable pageable);

    List<Vinculo_Caso> findByAutorizadoFalse(Pageable pageable);

    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.autorizado = false AND vc.caso.firma.id = :firmaId")
    List<Vinculo_Caso> findPendientesAutorizacionPorFirma(@Param("firmaId") Integer firmaId);

    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.autorizado = false AND vc.caso.firma.id = :firmaId")
    List<Vinculo_Caso> findPendientesAutorizacionPorFirma(@Param("firmaId") Integer firmaId, Pageable pageable);
}