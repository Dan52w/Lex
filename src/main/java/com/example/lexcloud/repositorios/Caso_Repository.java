package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Caso_Repository extends JpaRepository<Caso, Integer> {

    // 1. Encontrar casos vinculados a una persona (Abogado o Cliente) y autorizados
    // Esta es la query principal para la seguridad de LexCloud
    @Query("SELECT c FROM Caso c " +
            "JOIN Vinculo_Caso vc ON vc.caso.id = c.id " +
            "WHERE vc.personaFirma.id = :personaId " + // id de la persona que consulta
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findCasosAutorizadosPorPersona(@Param("personaId") Integer personaId);

    @Query("SELECT c FROM Caso c JOIN Vinculo_Caso vc ON vc.caso.id = c.id " +
            "WHERE vc.personaFirma.id = :personaId AND LOWER(c.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))")
    List<Caso> buscarPorTituloParcial(@Param("titulo") String titulo, @Param("personaId") Integer personaId);

    // 4. Listar casos por Estado (ej: 'Abierto', 'Cerrado') que pertenezcan al usuario
    @Query("SELECT c FROM Caso c " +
            "JOIN Vinculo_Caso vc ON vc.caso.id = c.id " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.nombre = :estadoNombre " +
            "AND vc.autorizado = true")
    List<Caso> findByEstadoAndPersona(@Param("estadoNombre") String estadoNombre, @Param("personaId") Integer personaId);

    // 5. Filtrar por ID del Subtipo de Caso
    // Útil cuando el usuario selecciona el subtipo desde un dropdown (select)
    @Query("SELECT c FROM Caso c " +
            "JOIN Vinculo_Caso vc ON vc.caso.id = c.id " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.autorizado = true")
    List<Caso> findBySubtipoIdAndPersona(@Param("subtipoId") Integer subtipoId,
                                         @Param("personaId") Integer personaId);

    // 6. Filtro Combinado: Por Estado Y por Subtipo
    // Para búsquedas muy específicas en el panel de LexCloud
    @Query("SELECT c FROM Caso c " +
            "JOIN Vinculo_Caso vc ON vc.caso.id = c.id " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.id = :estadoId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.autorizado = true")
    List<Caso> findByEstadoAndSubtipoAndPersona(@Param("personaId") Integer personaId,
                                                @Param("estadoId") Integer estadoId,
                                                @Param("subtipoId") Integer subtipoId);

    // Buscar subcasos de un caso principal
    List<Caso> findByCasoPadreId(Integer padreId);
}