package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.EstadoVinculo;
import com.example.lexcloud.entidades.Vinculo_Caso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VinculoCaso_Repository extends JpaRepository<Vinculo_Caso, Integer> {

    /** Nombre del tipo de vínculo principal en el catálogo (H12). */
    String VINCULO_PRINCIPAL = "Principal";

    // =========================================================================
    // H11: JPQL compartida entre la variante normal y la paginada.
    // Cada consulta duplicada vive en una constante Q_* y la variante paginada
    // añade el orden determinista (Q_*_PAG), como en Caso_Repository.
    // Las constantes son expresiones constantes en tiempo de compilación,
    // válidas como valor de @Query.
    // =========================================================================

    String Q_PERSONAL_ASIGNADO_AL_CASO =
            "SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma p " +
            "JOIN FETCH vc.vinculo v " +
            "WHERE vc.caso.id = :casoId AND vc.estado = :estado";
    String Q_PERSONAL_ASIGNADO_AL_CASO_PAG = Q_PERSONAL_ASIGNADO_AL_CASO + " ORDER BY vc.id";

    String Q_PRINCIPALES_DEL_CASO =
            "SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = :principal AND vc.estado = :estado";
    String Q_PRINCIPALES_DEL_CASO_PAG = Q_PRINCIPALES_DEL_CASO + " ORDER BY vc.id";

    String Q_COUNT_PRINCIPALES_DEL_CASO =
            "SELECT COUNT(vc) FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = :principal";

    String Q_VINCULOS_DEL_CASO =
            "SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma " +
            "JOIN FETCH vc.vinculo " +
            "JOIN FETCH vc.cliente " +
            "JOIN FETCH vc.caso " +
            "WHERE vc.caso.id = :casoId " +
            "ORDER BY CASE WHEN vc.vinculo.nombre = :principal THEN 0 ELSE 1 END";
    String Q_VINCULOS_DEL_CASO_PAG = Q_VINCULOS_DEL_CASO + ", vc.id";

    String Q_PENDIENTES_POR_FIRMA =
            "SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.estado = com.example.lexcloud.entidades.EstadoVinculo.PENDIENTE " +
            "AND vc.caso.firma.id = :firmaId";
    String Q_PENDIENTES_POR_FIRMA_PAG = Q_PENDIENTES_POR_FIRMA + " ORDER BY vc.id";

    String Q_PENDIENTES_POR_PERSONA =
            "SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.estado = com.example.lexcloud.entidades.EstadoVinculo.PENDIENTE " +
            "AND EXISTS (SELECT 1 FROM Vinculo_Caso vc2 " +
            "WHERE vc2.caso.id = vc.caso.id " +
            "AND vc2.personaFirma.id = :personaId " +
            "AND vc2.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO) " +
            "ORDER BY vc.id";

    String Q_PENDIENTES_GLOBAL =
            "SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.estado = com.example.lexcloud.entidades.EstadoVinculo.PENDIENTE";
    String Q_PENDIENTES_GLOBAL_PAG = Q_PENDIENTES_GLOBAL + " ORDER BY vc.id";

    // 1. Verificar si un abogado/personal tiene acceso autorizado a un caso
    // (M13: por estado, no por flag). La usa ContextoSeguridad.
    boolean existsByPersonaFirma_IdAndCaso_IdAndEstado(
            Integer personaId, Integer casoId, EstadoVinculo estado);

    // 2b. Verificar si ya existe un vínculo para la misma persona, cliente y caso
    // (la BD tiene UNIQUE sobre esa terna; el servicio lo comprueba antes para
    // devolver 409 con mensaje claro en vez de la violación genérica).
    boolean existsByPersonaFirma_IdAndCliente_IdAndCaso_Id(Integer personaId, Integer clienteId, Integer casoId);

    // 3. Listar los abogados/personal asignados a un caso (solo vínculos con el
    // estado indicado; el servicio pide AUTORIZADO). Optimizado con FETCH.
    @Query(Q_PERSONAL_ASIGNADO_AL_CASO)
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(
            @Param("casoId") Integer casoId, @Param("estado") EstadoVinculo estado);

    @Query(Q_PERSONAL_ASIGNADO_AL_CASO_PAG)
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(
            @Param("casoId") Integer casoId, @Param("estado") EstadoVinculo estado, Pageable pageable);

    // 4. Listar todos los casos donde un abogado/personal está vinculado
    // ("Mis Casos" para el abogado), por estado (el servicio pide AUTORIZADO).
    List<Vinculo_Caso> findByPersonaFirma_IdAndEstado(Integer personaId, EstadoVinculo estado);

    List<Vinculo_Caso> findByPersonaFirma_IdAndEstado(
            Integer personaId, EstadoVinculo estado, Pageable pageable);

    // 4b. Variante de "Mis Casos" con alcance por firma (M2): el filtro por
    // firma del administrador se hace a nivel JPQL, no en memoria después
    // de paginar.
    List<Vinculo_Caso> findByPersonaFirma_IdAndEstadoAndCaso_Firma_Id(
            Integer personaId, EstadoVinculo estado, Integer firmaId, Pageable pageable);

    // =========================================================================
    // Métodos Administrativos y de Control
    // =========================================================================

    // 8. Principales de un caso con el estado indicado: si por datos viejos
    // hubiera dos vínculos 'Principal', la lista permite tomar el primero
    // sin romper (la guarda A8 en el servicio impide que se creen dos
    // 'Principal' por caso a partir de ahora).
    @Query(Q_PRINCIPALES_DEL_CASO)
    List<Vinculo_Caso> findPrincipalesDelCaso(
            @Param("casoId") Integer casoId,
            @Param("estado") EstadoVinculo estado,
            @Param("principal") String principal);

    // 8b. Cuenta TODOS los vínculos 'Principal' del caso, en cualquier
    // estado (A8): el servicio rechaza un segundo Principal antes del save;
    // el trigger V4 es la red de seguridad.
    @Query(Q_COUNT_PRINCIPALES_DEL_CASO)
    long countPrincipalesDelCaso(
            @Param("casoId") Integer casoId, @Param("principal") String principal);

    // 9. Todos los vínculos de un caso (el 'Principal' primero). Reemplaza al
    // antiguo findByCaso_Id, que devolvía Optional y rompía con 500 cuando un
    // caso tenía N vínculos (SVC-1): el servicio toma el primero.
    @Query(Q_VINCULOS_DEL_CASO)
    List<Vinculo_Caso> findVinculosDelCaso(
            @Param("casoId") Integer casoId, @Param("principal") String principal);

    // 9b. Variante paginada (M1): obtenerCliente/obtenerAbogadoPrincipal no
    // deben cargar la lista entera para tomar el primero.
    @Query(Q_VINCULOS_DEL_CASO_PAG)
    List<Vinculo_Caso> findVinculosDelCaso(
            @Param("casoId") Integer casoId, @Param("principal") String principal, Pageable pageable);

    // 10. Listar vínculos pendientes de autorización de una firma
    // (para el panel de administración). La rama global sin firma (C8)
    // desaparece: nadie ve pendientes de todas las firmas.
    @Query(Q_PENDIENTES_POR_FIRMA)
    List<Vinculo_Caso> findPendientesAutorizacionPorFirma(@Param("firmaId") Integer firmaId);

    @Query(Q_PENDIENTES_POR_FIRMA_PAG)
    List<Vinculo_Caso> findPendientesAutorizacionPorFirma(
            @Param("firmaId") Integer firmaId, Pageable pageable);

    // 10b. Pendientes de autorización visibles para una persona sin firma
    // resoluble (M26): pendientes de casos en los que la persona tiene un
    // vínculo AUTORIZADO. El personal sin registro de abogado accede por
    // vínculo, no por firma, así que nunca se le devuelve un 403 aquí.
    @Query(Q_PENDIENTES_POR_PERSONA)
    List<Vinculo_Caso> findPendientesPorPersona(
            @Param("personaId") Integer personaId, Pageable pageable);

    // 10c. V5: TODOS los vínculos pendientes (solo súper admin, global).
    // La rama global sin firma (C8) sigue cerrada para el resto.
    @Query(Q_PENDIENTES_GLOBAL_PAG)
    List<Vinculo_Caso> findPendientesAutorizacion(Pageable pageable);
}
