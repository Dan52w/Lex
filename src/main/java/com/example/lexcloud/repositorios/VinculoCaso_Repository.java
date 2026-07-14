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

    // 1. Verificar si un abogado/personal tiene acceso autorizado a un caso
    // Corregido a PersonaFirma_Id y Caso_Id para evitar fallos de mapeo en Spring Data
    boolean existsByPersonaFirma_IdAndCaso_IdAndAutorizadoTrue(Integer personaId, Integer casoId);

    // 2. Verificar si un cliente específico está vinculado a un caso
    // CORREGIDO: Ahora busca por Cliente_Id de la nueva entidad Cliente
    boolean existsByCliente_IdAndCaso_IdAndAutorizadoTrue(Integer clienteId, Integer casoId);

    // 3. Listar todos los abogados/personal asignados a un caso (Optimizado con FETCH)
    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.personaFirma p " +
            "JOIN FETCH vc.vinculo v " +
            "WHERE vc.caso.id = :casoId AND vc.autorizado = true")
    List<Vinculo_Caso> findPersonalAsignadoAlCaso(@Param("casoId") Integer casoId);

    // 4. Listar todos los casos donde un abogado/personal está vinculado ("Mis Casos" para el abogado)
    List<Vinculo_Caso> findByPersonaFirma_IdAndAutorizadoTrue(Integer personaId);

    // =========================================================================
    // NUEVOS MÉTODOS: Búsqueda y gestión por CLIENTE
    // =========================================================================

    // 5. Listar todos los casos donde un Cliente está vinculado (Para el portal de clientes o perfil del cliente)
    List<Vinculo_Caso> findByCliente_IdAndAutorizadoTrue(Integer clienteId);

    // 6. Listar casos del cliente optimizado (Trae la información del caso en una sola consulta para evitar N+1)
    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "JOIN FETCH vc.caso c " +
            "JOIN FETCH vc.vinculo v " +
            "WHERE vc.cliente.id = :clienteId AND vc.autorizado = true")
    List<Vinculo_Caso> findCasosPorClienteConDetalles(@Param("clienteId") Integer clienteId);

    // 7. Buscar si existe algún vínculo (activo o no) de un cliente con un caso específico
    Optional<Vinculo_Caso> findByCliente_IdAndCaso_Id(Integer clienteId, Integer casoId);

    // =========================================================================
    // Métodos Administrativos y de Control
    // =========================================================================

    // 8. Buscar quién es el abogado principal de un caso
    @Query("SELECT vc FROM Vinculo_Caso vc WHERE vc.caso.id = :casoId " +
            "AND vc.vinculo.nombre = 'Principal' AND vc.autorizado = true")
    Optional<Vinculo_Caso> findAbogadoPrincipalDelCaso(@Param("casoId") Integer casoId);

    // 9. Buscar vínculo por ID de caso (Corregido a Caso_Id)
    Optional<Vinculo_Caso> findByCaso_Id(Integer idCaso);

    // 10. Listar vínculos pendientes de autorización (Para panel de administración)
    List<Vinculo_Caso> findByAutorizadoFalse();

    // 11. Listar vínculos activos de un caso donde el abogado siga activo en la firma
    @Query("SELECT vc FROM Vinculo_Caso vc " +
            "WHERE vc.caso.id = :casoId " +
            "AND vc.personaFirma.activo = true " +
            "AND vc.autorizado = true")
    List<Vinculo_Caso> findVinculosActivosPorCaso(@Param("casoId") Integer casoId);
}