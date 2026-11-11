package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Caso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Caso_Repository extends JpaRepository<Caso, Integer> {

    /**
     * M17 (lote 6): ¿existe algún caso con este subtipo? Prohíbe cambiar el
     * tipoCaso de un subtipo que ya tiene casos asociados. Derivado con el
     * mismo patrón con guion bajo que findByAbogado_Id / existsByAbogado_Id
     * de EquipoAbogado_Repository.
     */
    boolean existsBySubtipo_Caso_Id(Integer subtipoId);


    // =========================================================================
    // H11: JPQL compartida entre la variante normal y la paginada.
    // Cada consulta duplicada vive en una constante Q_* y la variante paginada
    // añade " ORDER BY c.id" (H7) con Q_*_PAG. Las constantes son expresiones
    // constantes en tiempo de compilación, válidas como valor de @Query.
    // =========================================================================

    String Q_CASOS_AUTORIZADOS_POR_PERSONA =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_CASOS_AUTORIZADOS_POR_PERSONA_PAG = Q_CASOS_AUTORIZADOS_POR_PERSONA + " ORDER BY c.id";

    String Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaIdFirma " +
            "AND vc.cliente.id = :clienteId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_PAG = Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE + " ORDER BY c.id";

    String Q_SUPER_BUSCADOR =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true " +
            "AND (LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\' " +
            "     OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\' " +
            "     OR c.radicadoInterno LIKE CONCAT('%', :termino, '%') ESCAPE '\\' " +
            "     OR c.radicadoJudicial LIKE CONCAT('%', :termino, '%') ESCAPE '\\')";
    String Q_SUPER_BUSCADOR_PAG = Q_SUPER_BUSCADOR + " ORDER BY c.id";

    String Q_BUSCAR_POR_TITULO_PARCIAL =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true " +
            "AND LOWER(c.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')) ESCAPE '\\'";
    String Q_BUSCAR_POR_TITULO_PARCIAL_PAG = Q_BUSCAR_POR_TITULO_PARCIAL + " ORDER BY c.id";

    String Q_POR_ESTADO_ID_Y_PERSONA =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.id = :estadoId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_POR_ESTADO_ID_Y_PERSONA_PAG = Q_POR_ESTADO_ID_Y_PERSONA + " ORDER BY c.id";

    String Q_POR_SUBTIPO_Y_PERSONA =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_POR_SUBTIPO_Y_PERSONA_PAG = Q_POR_SUBTIPO_Y_PERSONA + " ORDER BY c.id";

    String Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND c.estado.id = :estadoId " +
            "AND c.subtipo_Caso.id = :subtipoId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_PAG = Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA + " ORDER BY c.id";

    String Q_POR_CLIENTE_Y_PERSONA =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.cliente.id = :clienteId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true";
    String Q_POR_CLIENTE_Y_PERSONA_PAG = Q_POR_CLIENTE_Y_PERSONA + " ORDER BY c.id";

    String Q_BUSCAR_POR_CLIENTE =
            "SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "JOIN vc.cliente cl " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true " +
            "AND (LOWER(cl.nombres) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\' " +
            "     OR LOWER(cl.primerApellido) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\' " +
            "     OR LOWER(cl.segundoApellido) LIKE LOWER(CONCAT('%', :termino, '%')) ESCAPE '\\' " +
            "     OR cl.identificacion LIKE CONCAT('%', :termino, '%') ESCAPE '\\')";
    String Q_BUSCAR_POR_CLIENTE_PAG = Q_BUSCAR_POR_CLIENTE + " ORDER BY c.id";

    String Q_POR_CASO_PADRE =
            "SELECT c FROM Caso c " +
            "WHERE c.casoPadre.id = :padreId " +
            "AND c.activo = true";
    String Q_POR_CASO_PADRE_PAG = Q_POR_CASO_PADRE + " ORDER BY c.id";

    // =========================================================================
    // M2 (lote 7): variantes por firma de las consultas anteriores. La rama
    // admin de los listados usa estas variantes (mismo criterio por persona
    // y vínculo, más el filtro c.firma.id = :firmaId a nivel BD) en lugar
    // del post-filtro en memoria. Solo existen en variante paginada: los
    // servicios siempre las llaman con Pageable.
    // Nota de migración: el antiguo flag 'autorizado' del vínculo se
    // sustituyó por el estado (ver Vinculo_Caso.estado); ninguna consulta
    // de este archivo lo referencia ya.
    // =========================================================================

    String Q_CASOS_AUTORIZADOS_POR_PERSONA_FIRMA =
            Q_CASOS_AUTORIZADOS_POR_PERSONA + " AND c.firma.id = :firmaId";
    String Q_CASOS_AUTORIZADOS_POR_PERSONA_FIRMA_PAG = Q_CASOS_AUTORIZADOS_POR_PERSONA_FIRMA + " ORDER BY c.id";

    String Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_FIRMA =
            Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE + " AND c.firma.id = :firmaId";
    String Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_FIRMA_PAG = Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_FIRMA + " ORDER BY c.id";

    String Q_SUPER_BUSCADOR_FIRMA =
            Q_SUPER_BUSCADOR + " AND c.firma.id = :firmaId";
    String Q_SUPER_BUSCADOR_FIRMA_PAG = Q_SUPER_BUSCADOR_FIRMA + " ORDER BY c.id";

    String Q_BUSCAR_POR_TITULO_PARCIAL_FIRMA =
            Q_BUSCAR_POR_TITULO_PARCIAL + " AND c.firma.id = :firmaId";
    String Q_BUSCAR_POR_TITULO_PARCIAL_FIRMA_PAG = Q_BUSCAR_POR_TITULO_PARCIAL_FIRMA + " ORDER BY c.id";

    String Q_POR_ESTADO_ID_Y_PERSONA_FIRMA =
            Q_POR_ESTADO_ID_Y_PERSONA + " AND c.firma.id = :firmaId";
    String Q_POR_ESTADO_ID_Y_PERSONA_FIRMA_PAG = Q_POR_ESTADO_ID_Y_PERSONA_FIRMA + " ORDER BY c.id";

    String Q_POR_SUBTIPO_Y_PERSONA_FIRMA =
            Q_POR_SUBTIPO_Y_PERSONA + " AND c.firma.id = :firmaId";
    String Q_POR_SUBTIPO_Y_PERSONA_FIRMA_PAG = Q_POR_SUBTIPO_Y_PERSONA_FIRMA + " ORDER BY c.id";

    String Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_FIRMA =
            Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA + " AND c.firma.id = :firmaId";
    String Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_FIRMA_PAG = Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_FIRMA + " ORDER BY c.id";

    String Q_POR_CLIENTE_Y_PERSONA_FIRMA =
            Q_POR_CLIENTE_Y_PERSONA + " AND c.firma.id = :firmaId";
    String Q_POR_CLIENTE_Y_PERSONA_FIRMA_PAG = Q_POR_CLIENTE_Y_PERSONA_FIRMA + " ORDER BY c.id";

    String Q_BUSCAR_POR_CLIENTE_FIRMA =
            Q_BUSCAR_POR_CLIENTE + " AND c.firma.id = :firmaId";
    String Q_BUSCAR_POR_CLIENTE_FIRMA_PAG = Q_BUSCAR_POR_CLIENTE_FIRMA + " ORDER BY c.id";

    /**
     * Recupera todos los casos activos con vínculo en estado AUTORIZADO
     * para un usuario (persona) específico.
     * Este es el filtro de seguridad base de la aplicación.
     *
     * DISTINCT: el JOIN con vinculo_Casos puede devolver el mismo caso varias veces
     * si la persona tiene más de un vínculo hacia él; sin DISTINCT la lista
     * traería duplicados (y rompería las búsquedas Optional por radicado).
     */
    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_PERSONA)
    List<Caso> findCasosAutorizadosPorPersona(@Param("personaId") Integer personaId);

    /**
     * Valida que exista relación autorizada entre un abogado (personaFirma) y un cliente (cliente).
     * H13: el parámetro se llamaba personaIdCliente aunque es el id del cliente.
     */
    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE)
    List<Caso> findCasosAutorizadosPorAbogadoYCliente(
            @Param("personaIdFirma") Integer personaIdFirma,
            @Param("clienteId") Integer clienteId);

    /**
     * Búsqueda exacta por Radicado Interno.
     */
    @Query("SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
            "AND c.activo = true " +
            "AND c.radicadoInterno = :radicadoInterno")
    Optional<Caso> findByRadicadoInternoAndPersona(
            @Param("radicadoInterno") String radicadoInterno,
            @Param("personaId") Integer personaId);

    /**
     * Búsqueda exacta por Radicado Judicial.
     */
    @Query("SELECT DISTINCT c FROM Caso c " +
            "JOIN c.vinculo_Casos vc " +
            "WHERE vc.personaFirma.id = :personaId " +
            "AND vc.estado = com.example.lexcloud.entidades.EstadoVinculo.AUTORIZADO " +
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
     * Garantizando que solo se muestren resultados con vínculo en estado
     * AUTORIZADO para el usuario.
     *
     * ESCAPE '\': el término viene parametrizado, pero los caracteres %, _ y \
     * dentro del término seguirían actuando como comodines LIKE; el ESCAPE
     * permite que el servicio los neutralice escapándolos antes de llamar.
     */
    @Query(Caso_Repository.Q_SUPER_BUSCADOR)
    List<Caso> superBuscador(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId);

    /**
     * Búsqueda por coincidencia parcial solo en el título del caso.
     */
    @Query(Caso_Repository.Q_BUSCAR_POR_TITULO_PARCIAL)
    List<Caso> buscarPorTituloParcial(
            @Param("titulo") String titulo,
            @Param("personaId") Integer personaId);

    /**
     * H15: variante por id de estado (misma semántica que la antigua
     * findByEstadoAndPersona por nombre, ya borrada — sin llamadores).
     * Los llamadores deben usar esta; el servicio resuelve el id antes.
    @Query(Caso_Repository.Q_POR_ESTADO_ID_Y_PERSONA)
    List<Caso> findByEstadoIdAndPersona(
            @Param("estadoId") Integer estadoId,
            @Param("personaId") Integer personaId);

    /**
     * Filtrar casos por ID del Subtipo de Caso.
     */
    @Query(Caso_Repository.Q_POR_SUBTIPO_Y_PERSONA)
    List<Caso> findBySubtipoIdAndPersona(
            @Param("subtipoId") Integer subtipoId,
            @Param("personaId") Integer personaId);

    /**
     * Filtro combinado: Por Estado Y por Subtipo de Caso simultáneamente.
     */
    @Query(Caso_Repository.Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA)
    List<Caso> findByEstadoAndSubtipoAndPersona(
            @Param("personaId") Integer personaId,
            @Param("estadoId") Integer estadoId,
            @Param("subtipoId") Integer subtipoId);

    // =========================================================================
    // NUEVOS MÉTODOS: BÚSQUEDA POR CLIENTE
    // =========================================================================

    /**
     * Lista todos los casos de un cliente específico por su ID,
     * garantizando que el usuario consultor tenga un vínculo en estado AUTORIZADO.
     */
    @Query(Caso_Repository.Q_POR_CLIENTE_Y_PERSONA)
    List<Caso> findByClienteIdAndPersona(
            @Param("clienteId") Integer clienteId,
            @Param("personaId") Integer personaId);

    /**
     * Busca casos por coincidencias parciales en el nombre, apellidos
     * o número de identificación del cliente asignado.
     */
    @Query(Caso_Repository.Q_BUSCAR_POR_CLIENTE)
    List<Caso> buscarPorClienteNombreOIdentificacion(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId);
    // =========================================================================

    /**
     * Encuentra todos los subcasos directos de un caso principal.
     * H2: antes era un derivado sin filtro; ahora es @Query con
     * c.activo = true (mismo nombre y firma).
     */
    @Query(Caso_Repository.Q_POR_CASO_PADRE)
    List<Caso> findByCasoPadreId(@Param("padreId") Integer padreId);

    // =========================================================================
    // VARIANTES PAGINADAS
    // Misma consulta que arriba, con Pageable para no traer listas sin límite
    // cuando la persona o la firma acumulan muchos casos. Los servicios deben
    // migrar los listados a estas variantes.
    // H7: cada variante paginada lleva ORDER BY c.id (constantes Q_*_PAG).
    // =========================================================================

    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_PERSONA_PAG)
    List<Caso> findCasosAutorizadosPorPersona(@Param("personaId") Integer personaId, Pageable pageable);

    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_PAG)
    List<Caso> findCasosAutorizadosPorAbogadoYCliente(
            @Param("personaIdFirma") Integer personaIdFirma,
            @Param("clienteId") Integer clienteId,
            Pageable pageable);

    @Query(Caso_Repository.Q_SUPER_BUSCADOR_PAG)
    List<Caso> superBuscador(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_BUSCAR_POR_TITULO_PARCIAL_PAG)
    List<Caso> buscarPorTituloParcial(
            @Param("titulo") String titulo,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_ESTADO_ID_Y_PERSONA_PAG)
    List<Caso> findByEstadoIdAndPersona(
            @Param("estadoId") Integer estadoId,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_SUBTIPO_Y_PERSONA_PAG)
    List<Caso> findBySubtipoIdAndPersona(
            @Param("subtipoId") Integer subtipoId,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_PAG)
    List<Caso> findByEstadoAndSubtipoAndPersona(
            @Param("personaId") Integer personaId,
            @Param("estadoId") Integer estadoId,
            @Param("subtipoId") Integer subtipoId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_CLIENTE_Y_PERSONA_PAG)
    List<Caso> findByClienteIdAndPersona(
            @Param("clienteId") Integer clienteId,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_BUSCAR_POR_CLIENTE_PAG)
    List<Caso> buscarPorClienteNombreOIdentificacion(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId,
            Pageable pageable);

    // H6: variante paginada de findByCasoPadreId (devuelve List, no Page).
    @Query(Caso_Repository.Q_POR_CASO_PADRE_PAG)
    List<Caso> findByCasoPadreId(@Param("padreId") Integer padreId, Pageable pageable);

    // =========================================================================
    // M2 (lote 7): variantes por firma (rama admin de los listados).
    // Misma semántica que la variante por persona, más el filtro por firma
    // a nivel BD; sustituyen al post-filtro en memoria del servicio.
    // =========================================================================

    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_PERSONA_FIRMA_PAG)
    List<Caso> findCasosAutorizadosPorPersonaYFirma(
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_CASOS_AUTORIZADOS_POR_ABOGADO_Y_CLIENTE_FIRMA_PAG)
    List<Caso> findCasosAutorizadosPorAbogadoYClienteYFirma(
            @Param("personaIdFirma") Integer personaIdFirma,
            @Param("clienteId") Integer clienteId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_SUPER_BUSCADOR_FIRMA_PAG)
    List<Caso> superBuscadorYFirma(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_BUSCAR_POR_TITULO_PARCIAL_FIRMA_PAG)
    List<Caso> buscarPorTituloParcialYFirma(
            @Param("titulo") String titulo,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_ESTADO_ID_Y_PERSONA_FIRMA_PAG)
    List<Caso> findByEstadoIdAndPersonaYFirma(
            @Param("estadoId") Integer estadoId,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_SUBTIPO_Y_PERSONA_FIRMA_PAG)
    List<Caso> findBySubtipoIdAndPersonaYFirma(
            @Param("subtipoId") Integer subtipoId,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_ESTADO_Y_SUBTIPO_Y_PERSONA_FIRMA_PAG)
    List<Caso> findByEstadoAndSubtipoAndPersonaYFirma(
            @Param("personaId") Integer personaId,
            @Param("estadoId") Integer estadoId,
            @Param("subtipoId") Integer subtipoId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_POR_CLIENTE_Y_PERSONA_FIRMA_PAG)
    List<Caso> findByClienteIdAndPersonaYFirma(
            @Param("clienteId") Integer clienteId,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    @Query(Caso_Repository.Q_BUSCAR_POR_CLIENTE_FIRMA_PAG)
    List<Caso> buscarPorClienteNombreOIdentificacionYFirma(
            @Param("termino") String termino,
            @Param("personaId") Integer personaId,
            @Param("firmaId") Integer firmaId,
            Pageable pageable);

    /**
     * M11: chequeo de unicidad pre-save del radicado judicial (el DTO lo
     * exige con 23 dígitos; si viene, se comprueba antes del insert y el
     * save va además dentro de try/catch por la UNIQUE de BD).
     */
    boolean existsByRadicadoJudicial(String radicadoJudicial);

    /**
     * Siguiente valor de la secuencia de radicados internos (V4, C5).
     * La secuencia arranca en 1000000000 para no colisionar con los
     * radicados históricos derivados del id del caso.
     */
    @Query(value = "SELECT nextval('radicado_seq')", nativeQuery = true)
    long siguienteRadicado();
}
