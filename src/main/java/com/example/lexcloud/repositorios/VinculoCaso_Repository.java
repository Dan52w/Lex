package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Vinculo_Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VinculoCaso_Repository extends JpaRepository<Vinculo_Caso, Integer> {
    // 1. Verificar si una persona específica tiene acceso a un caso y está autorizada
    // Se usa antes de dejar que alguien abra un archivo o suba un documento
    boolean existsByPersonaFirmaAndCasoIdAndAutorizadoTrue(Integer personaId, Integer casoId);

    // 2. Verificar si un cliente específico está vinculado a un caso
    boolean existsByClienteIdAndCasoIdAndAutorizadoTrue(Integer clienteId, Integer casoId);

    // 3. Listar todos los abogados/personal asignados a un caso
    @Query("SELECT vc FROM Vinculo_Caso vc JOIN FETCH vc.personaFirma p " +
            "WHERE vc.caso.id = :casoId AND vc.autorizado = true")
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(@Param("casoId") Integer casoId);

    // 4. Listar todos los casos donde una persona está vinculada
    // (Ideal para el "Mis Casos" del panel del abogado)
    List<Vinculo_Caso> findByPersonaFirmaAndAutorizadoTrue(Integer personaId);

    // 5. Buscar quién es el abogado principal de un caso
    // Asumiendo que el nombre del vínculo es 'Principal'
    @Query("SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = 'Principal' AND vc.autorizado = true")
    Optional<Vinculo_Caso> findAbogadoPrincipalDelCaso(@Param("casoId") Integer casoId);

    // 6. Listar vínculos pendientes de autorización
    // Útil para un administrador que debe aprobar quién accede a qué información
    List<Vinculo_Caso> findByAutorizadoFalse();

    // 7. Buscar vínculos de personas que siguen activas en la firma
    @Query("SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.personaFirma.activo = true AND vc.autorizado = true")
    List<Vinculo_Caso> findVinculosActivosPorCaso(@Param("casoId") Integer casoId);
}
