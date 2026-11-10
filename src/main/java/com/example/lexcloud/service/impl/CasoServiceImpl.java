package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.CasoMapper;
import com.example.lexcloud.dto.request.CasoRequestDTO;
import com.example.lexcloud.dto.response.CasoResponseDTO;
import com.example.lexcloud.entidades.*;
import com.example.lexcloud.repositorios.*;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.CasoService;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CasoServiceImpl implements CasoService {

    private final Caso_Repository casoRepository;
    private final Estado_Repository estadoRepository;
    private final SubtipoCaso_Repository subtipoRepository;
    private final Persona_Repository personaRepository;
    private final VinculoCaso_Repository vinculoCasoRepository;
    private final Vinculo_Repository vinculoRepository;
    private final Cliente_Repository clienteRepository;
    private final HistorialCambio_Repository historialCambioRepository;
    private final Firma_Repository firmaRepository;
    private final CasoMapper casoMapper;
    private final ContextoSeguridad contextoSeguridad;

    public CasoServiceImpl(Caso_Repository casoRepository,
                           Estado_Repository estadoRepository,
                           SubtipoCaso_Repository subtipoRepository,
                           Persona_Repository personaRepository,
                           VinculoCaso_Repository vinculoCasoRepository,
                           HistorialCambio_Repository historialCambioRepository,
                           Vinculo_Repository vinculoRepository,
                           Cliente_Repository clienteRepository,
                           Firma_Repository firmaRepository,
                           CasoMapper casoMapper,
                           ContextoSeguridad contextoSeguridad) {
        this.casoRepository = casoRepository;
        this.estadoRepository = estadoRepository;
        this.subtipoRepository = subtipoRepository;
        this.personaRepository = personaRepository;
        this.vinculoCasoRepository = vinculoCasoRepository;
        this.historialCambioRepository = historialCambioRepository;
        this.vinculoRepository = vinculoRepository;
        this.clienteRepository = clienteRepository;
        this.firmaRepository = firmaRepository;
        this.casoMapper = casoMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    @Transactional
    public CasoResponseDTO guardar(CasoRequestDTO request) {
        // El caso se radica en la firma del usuario autenticado.
        contextoSeguridad.verificarMismaFirma(request.idFirma());

        Caso caso = casoMapper.toEntity(request);

        caso.setEstado(estadoRepository.findById(request.idEstado())
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado")));
        caso.setSubtipo_Caso(subtipoRepository.findById(request.idSubtipo())
                .orElseThrow(() -> new EntityNotFoundException("Subtipo no encontrado")));

        Integer idPadreEfectivo = (request.idCasoPadre() != null) ? request.idCasoPadre() : 1;
        Caso padre = casoRepository.findById(idPadreEfectivo)
                .orElseThrow(() -> new EntityNotFoundException("Caso padre no encontrado (ID: " + idPadreEfectivo + ")"));
        caso.setCasoPadre(padre);

        caso.setFirma(firmaRepository.findById(request.idFirma())
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada")));

        boolean necesitaGenerarRadicadoInterno =
                (caso.getRadicadoInterno() == null || caso.getRadicadoInterno().trim().isEmpty());
        boolean necesitaGenerarRadicadoJudicial =
                (caso.getRadicadoJudicial() == null ||
                        caso.getRadicadoJudicial().trim().equals("0") ||
                        caso.getRadicadoJudicial().trim().isEmpty());

        // Inserción con radicados temporales, con reintentos ante colisión
        Caso casoGuardado = guardarConRadicadosTemporales(caso, necesitaGenerarRadicadoInterno, necesitaGenerarRadicadoJudicial);

        // Reemplazo de los valores temporales por los radicados definitivos según el ID real asignado
        if (necesitaGenerarRadicadoInterno) {
            casoGuardado.setRadicadoInterno(String.format("%010d", casoGuardado.getId()));
        }
        if (necesitaGenerarRadicadoJudicial) {
            String radInternoFinal = casoGuardado.getRadicadoInterno();
            casoGuardado.setRadicadoJudicial(String.format("%23s", radInternoFinal).replace(' ', '0'));
        }

        casoGuardado = casoRepository.save(casoGuardado);

        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.idCliente()));
        // El creador del vínculo 'Principal' es el usuario autenticado, no el
        // idPersonaCreador que viene en el DTO (el front lo envía, pero es un
        // dato del cliente: aceptar otro id crearía el vínculo a nombre de
        // otra persona). El vínculo nace autorizado porque el propio creador
        // autoriza su vínculo al radicar el caso (ver VinculoCasoServiceImpl).
        crearVinculo(casoGuardado, contextoSeguridad.idPersonaAutenticada(), cliente, true);

        return casoMapper.toResponse(casoGuardado);
    }

    @Override
    @Transactional
    public CasoResponseDTO actualizar(Integer id, CasoRequestDTO request) {
        if (id == 1) {
            throw new EntityNotFoundException("No se puede modificar el caso raíz");
        }
        contextoSeguridad.verificarAccesoACaso(id);

        return casoRepository.findById(id).map(oldCaso -> {
            String estadoAnterior = (oldCaso.getEstado() != null) ? oldCaso.getEstado().getNombre() : "Sin estado";
            //String subtipoCasoAnterior = (oldCaso.getSubtipo_Caso() != null) ? oldCaso.getSubtipo_Caso().getNombre() : "Sin definir";
            String nombreCasoAnterior = (oldCaso.getTitulo() != null) ? oldCaso.getTitulo() : "Sin titulo";

            // CAMPOS PERMITIDOS DE ACTUALIZAR:
            oldCaso.setTitulo(request.titulo());

            if (request.idEstado() != null) {
                oldCaso.setEstado(estadoRepository.findById(request.idEstado())
                        .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado")));
            }
            /* Ya no se permitira actualizar el Subtipo Caso -- Se guarda consulta en caso de que sea necesario
            if (request.idSubtipo() != null) {
                oldCaso.setSubtipo_Caso(subtipoRepository.findById(request.idSubtipo())
                        .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada")));
            }
            */
            // Actualización condicional del Radicado Judicial (si viene "0", re-calcula a 23 dígitos)
            if (request.radicadoJudicial() != null && !request.radicadoJudicial().trim().isEmpty()) {
                if ("0".equals(request.radicadoJudicial().trim())) {
                    String radInterno = oldCaso.getRadicadoInterno();
                    oldCaso.setRadicadoJudicial(String.format("%23s", radInterno).replace(' ', '0'));
                } else {
                    oldCaso.setRadicadoJudicial(request.radicadoJudicial());
                }
            }

            Caso casoActualizado = casoRepository.save(oldCaso);

            // El autor del historial es el usuario autenticado, no el
            // idPersonaCreador del DTO (el front lo envía con fallback a 1,
            // la persona del sistema, cuando no lo resuelve).
            registrarCambioEnHistorial(casoActualizado, contextoSeguridad.idPersonaAutenticada(),
                    "Actualización de datos. Estado anterior: " + estadoAnterior + ". Título anterior: " + nombreCasoAnterior);
            return casoMapper.toResponse(casoActualizado);

        }).orElseThrow(() -> new EntityNotFoundException("Caso no encontrado con ID: " + id));
    }

    // Métodos de consulta...
    @Override
    public List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona) {
        return listarVinculadosPersonaAutorizado(idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarVinculadosPersonaAutorizado(Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.findCasosAutorizadosPorPersona(idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente) {
        return listarCasosAutorizadosPorAbogadoYCliente(idPersonaFirma, idCliente, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarCasosAutorizadosPorAbogadoYCliente(Integer idPersonaFirma, Integer idCliente, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersonaFirma);
        return filtrarAdminSiAplica(
                casoRepository.findCasosAutorizadosPorAbogadoYCliente(idPersonaFirma, idCliente, Paginacion.crear(page, size)),
                idPersonaFirma);
    }

    @Override
    public List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona) {
        return listarPorTitulo(titulo, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorTitulo(String titulo, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.buscarPorTituloParcial(BusquedaUtil.escaparLike(titulo), idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona) {
        return listarPorEstado(estadoNombre, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorEstado(String estadoNombre, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.findByEstadoAndPersona(estadoNombre, idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona) {
        return listarPorSubtipo(idSubtipo, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipo(Integer idSubtipo, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.findBySubtipoIdAndPersona(idSubtipo, idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo) {
        return listarPorSubtipoAndEstado(idPersona, idEstado, idSubtipo, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorSubtipoAndEstado(Integer idPersona, Integer idEstado, Integer idSubtipo, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.findByEstadoAndSubtipoAndPersona(idPersona, idEstado, idSubtipo, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarPorCasoPadre(Integer idCaso) {
        contextoSeguridad.verificarAccesoACaso(idCaso);
        return casoRepository.findByCasoPadreId(idCaso).stream()
                .filter(c -> c.getId() != 1)
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CasoResponseDTO buscarPorRadicadoInterno(String radicadoInterno, Integer idPersona) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return casoRepository.findByRadicadoInternoAndPersona(radicadoInterno, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso con radicado interno '" + radicadoInterno + "' no encontrado o no autorizado"));
    }

    @Override
    public CasoResponseDTO buscarPorRadicadoJudicial(String radicadoJudicial, Integer idPersona) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return casoRepository.findByRadicadoJudicialAndPersona(radicadoJudicial, idPersona)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso con radicado judicial '" + radicadoJudicial + "' no encontrado o no autorizado"));
    }

    @Override
    public List<CasoResponseDTO> superBuscador(String termino, Integer idPersona) {
        return superBuscador(termino, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> superBuscador(String termino, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.superBuscador(BusquedaUtil.escaparLike(termino), idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona) {
        return listarPorCliente(idCliente, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> listarPorCliente(Integer idCliente, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.findByClienteIdAndPersona(idCliente, idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona) {
        return buscarPorNombreOIdentificacionCliente(termino, idPersona, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<CasoResponseDTO> buscarPorNombreOIdentificacionCliente(String termino, Integer idPersona, int page, int size) {
        contextoSeguridad.verificarMismaPersona(idPersona);
        return filtrarAdminSiAplica(
                casoRepository.buscarPorClienteNombreOIdentificacion(BusquedaUtil.escaparLike(termino), idPersona, Paginacion.crear(page, size)),
                idPersona);
    }

    @Override
    public CasoResponseDTO buscarPorId(Integer id) {
        if (id == 1) {
            throw new EntityNotFoundException("Caso no encontrado");
        }
        contextoSeguridad.verificarAccesoACaso(id);
        return casoRepository.findById(id)
                .map(casoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
    }

    @Override
    public void desactivarCaso(Integer id) {
        if (id == 1) throw new IllegalArgumentException("No se puede desactivar el caso raíz del sistema");
        contextoSeguridad.verificarAccesoACaso(id);

        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado"));
        caso.setActivo(false);
        casoRepository.save(caso);
        // S4: la desactivación es solo el flag 'activo'. No hay borrado físico
        // de casos en ningún servicio (casoRepository.delete no se usa): el
        // cascade ALL de Caso.subCasos nunca se dispara desde esta capa, y el
        // trigger V3 de borrado lógico solo actúa sobre DELETE físico, que la
        // API no emite. La desactivación no se propaga a los subcasos: cada
        // subcaso se desactiva por separado, por diseño.
    }

    /**
     * Inserta el caso generando radicados temporales únicos.
     * Reintenta si el valor aleatorio temporal colisiona con uno existente
     * (poco probable, pero posible bajo alta concurrencia).
     */
    private Caso guardarConRadicadosTemporales(Caso caso, boolean generarInterno, boolean generarJudicial) {
        final int MAX_INTENTOS = 3;
        DataIntegrityViolationException ultimoError = null;

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            try {
                if (generarInterno) {
                    long tempNum = Math.abs((System.nanoTime() + intento) % 10_000_000_000L);
                    caso.setRadicadoInterno(String.format("%010d", tempNum));
                }
                if (generarJudicial) {
                    String radInternoTemp = caso.getRadicadoInterno();
                    caso.setRadicadoJudicial(String.format("%23s", radInternoTemp).replace(' ', '0'));
                }
                return casoRepository.saveAndFlush(caso);
            } catch (DataIntegrityViolationException ex) {
                ultimoError = ex; // reintenta con un nuevo valor temporal
            }
        }
        throw new IllegalStateException(
                "No se pudo generar un radicado único tras varios intentos, intente nuevamente.", ultimoError);
    }

    private void crearVinculo(Caso caso, Integer idPersona, Cliente cliente, boolean autorizado) {
        Persona persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new EntityNotFoundException("Persona creadora del caso no encontrada con ID: " + idPersona));
        // El vínculo 'Principal' se busca por nombre, no por id mágico: en
        // una base nueva solo existe 'SIN VÍNCULO' y el id 2 no está garantizado.
        Vinculo tipoVinculo = vinculoRepository.findByNombre("Principal")
                .orElseThrow(() -> new EntityNotFoundException(
                        "Vínculo 'Principal' no configurado en la base de datos"));

        Vinculo_Caso vinculoCaso = new Vinculo_Caso();
        vinculoCaso.setCaso(caso);
        vinculoCaso.setPersonaFirma(persona);
        vinculoCaso.setCliente(cliente);
        vinculoCaso.setVinculo(tipoVinculo);
        vinculoCaso.setAutorizado(autorizado);

        vinculoCasoRepository.save(vinculoCaso);
    }

    private void registrarCambioEnHistorial(Caso caso, Integer idPersona, String descripcion) {
        Persona persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con ID: " + idPersona));
        Historial_Cambio historial = new Historial_Cambio();
        historial.setCaso(caso);
        historial.setPersona(persona);
        historial.setDescripcion(descripcion);
        historial.setFecha(LocalDate.now());
        historialCambioRepository.save(historial);
    }

    /**
     * Cuando un administrador consulta los casos de otra persona, solo se le
     * devuelven los de su propia firma. La consulta ya viene paginada por
     * persona, así que el filtrado posterior puede acortar la página: es el
     * costo de no tener una variante por firma en el repositorio para estas
     * consultas (pendiente si crece el uso administrativo).
     */
    private List<CasoResponseDTO> filtrarAdminSiAplica(List<Caso> casos, Integer idPersonaConsultada) {
        List<Caso> filtrados = casos.stream()
                .filter(c -> c.getId() != 1)
                .collect(Collectors.toList());
        if (contextoSeguridad.esAdmin()
                && !idPersonaConsultada.equals(contextoSeguridad.idPersonaAutenticada())) {
            Integer firmaAdmin = contextoSeguridad.idFirmaAutenticada().orElse(null);
            filtrados = filtrados.stream()
                    .filter(c -> firmaAdmin != null
                            && c.getFirma() != null
                            && firmaAdmin.equals(c.getFirma().getId()))
                    .collect(Collectors.toList());
        }
        return filtrados.stream()
                .map(casoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
