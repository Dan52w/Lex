package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Caso_Repository extends JpaRepository<Caso, Integer> {

    /**
     * Recupera todos los casos activos y autorizados para un usuario (persona) específico.
     * Este es el filtro de seguridad base de la aplicación.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findCasosAutorizadosPorPersona(@Param("personaId") Integer personaId);

    /**
     * Valida que exista relación autorizada entre un abogado (personaFirma) y un cliente (cliente).
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaIdFirma " +
            "AND vc.cliente.id = :personaIdCliente " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findCasosAutorizadosPorAbogadoYCliente(
            @Param("personaIdFirma") Integer personaIdFirma,
            @Param("personaIdCliente") Integer personaIdCliente);

    /**
     * Búsqueda exacta por Radicado Interno.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true " +
            "AND c.radicadoInterno = :radicadoInterno")
    Optional<Caso> findByRadicadoInternoAndPersona(
            @Param("radicadoInterno") String radicadoInterno,
            @Param("personaId") Integer personaId);

    /**
     * Búsqueda exacta por Radicado Judicial.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true " +
            "AND c.radicadoJudicial = :radicadoJudicial")
    Optional<Caso> findByRadicadoJudicialAndPersona(
            @Param("radicadoJudicial") String radicadoJudicial,
            @Param("personaId") Integer personaId);

    /**
     * SUPER BUSCADOR: Busca coincidencias parciales (case-insensitive) en:
     * - Título del caso
     * - Descripción del caso
     * - Radicado Interno
     * - Radicado Judicial
     * Garantizando que solo se muestren resultados autorizados para el usuario.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true " +
            "AND (LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "     OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "     OR c.radicadoInterno LIKE CONCAT('%', :termino, '%') " +
            "     OR c.radicadoJudicial LIKE CONCAT('%', :termino, '%'))")
    List<Caso> superBuscador(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId);

    /**
     * Búsqueda por coincidencia parcial solo en el título del caso.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true " +
            "AND LOWER(c.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))")
    List<Caso> buscarPorTituloParcial(
            @Param("titulo") String titulo,
            @Param("personaId") Integer personaId);

    /**
     * Filtrar casos por nombre del Estado (ej: 'Abierto', 'Cerrado').
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.nombre = :estadoNombre " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findByEstadoAndPersona(
            @Param("estadoNombre") String estadoNombre,
            @Param("personaId") Integer personaId);

    /**
     * Filtrar casos por ID del Subtipo de Caso.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findBySubtipoIdAndPersona(
            @Param("subtipoId") Integer subtipoId,
            @Param("personaId") Integer personaId);

    /**
     * Filtro combinado: Por Estado Y por Subtipo de Caso simultáneamente.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.id = :estadoId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findByEstadoAndSubtipoAndPersona(
            @Param("personaId") Integer personaId,
            @Param("estadoId") Integer estadoId,
            @Param("subtipoId") Integer subtipoId);

    // =========================================================================
    // NUEVOS MÉTODOS: BÚSQUEDA POR CLIENTE
    // =========================================================================

    /**
     * Lista todos los casos de un cliente específico por su ID,
     * garantizando que el usuario consultor tenga acceso autorizado.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.cliente.id = :clienteId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true")
    List<Caso> findByClienteIdAndPersona(
            @Param("clienteId") Integer clienteId,
            @Param("personaId") Integer personaId);

    /**
     * Busca casos por coincidencias parciales en el nombre, apellidos
     * o número de identificación del cliente asignado.
     */
    @Query("SELECT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "JOIN vc.cliente cl " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.autorizado = true " +
            "AND c.activo = true " +
            "AND (LOWER(cl.nombres) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "     OR LOWER(cl.primerApellido) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "     OR LOWER(cl.segundoApellido) LIKE LOWER(CONCAT('%', :termino, '%')) " +
            "     OR cl.identificacion LIKE CONCAT('%', :termino, '%'))")
    List<Caso> buscarPorClienteNombreOIdentificacion(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId);
    // =========================================================================

    /**
     * Encuentra todos los subcasos directos de un caso principal.
     */
    List<Caso> findByCasoPadreId(Integer padreId);
}