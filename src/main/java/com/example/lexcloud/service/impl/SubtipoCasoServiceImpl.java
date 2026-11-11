package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.SubtipoCasoMapper;
import com.example.lexcloud.dto.request.SubtipoCasoRequestDTO;
import com.example.lexcloud.dto.response.SubtipoCasoResponseDTO;
import com.example.lexcloud.entidades.Subtipo_Caso;
import com.example.lexcloud.entidades.Tipo_Caso;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.SubtipoCaso_Repository;
import com.example.lexcloud.repositorios.TipoCaso_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbstractCatalogoService;
import com.example.lexcloud.service.SubtipoCasoService;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Lote 6 (M14/H1/A18/M17/M16): el flujo común vive en
 * {@link AbstractCatalogoService}; aquí solo las particularidades del subtipo:
 * unicidad por (tipoCaso, nombre), carga del Tipo_Caso ANTES de la
 * comprobación de duplicado, prohibición de cambiar el tipo de un subtipo
 * con casos, y ocultación de 'RAÍZ'.
 */
@Service
@Transactional
public class SubtipoCasoServiceImpl extends AbstractCatalogoService<Subtipo_Caso, SubtipoCasoRequestDTO, SubtipoCasoResponseDTO>
        implements SubtipoCasoService {

    private final SubtipoCaso_Repository subtipoRepository;
    private final TipoCaso_Repository tipoRepository;
    private final SubtipoCasoMapper subtipoCasoMapper;
    private final Caso_Repository casoRepository;

    public SubtipoCasoServiceImpl(SubtipoCaso_Repository subtipoRepository,
                                  TipoCaso_Repository tipoRepository,
                                  SubtipoCasoMapper subtipoCasoMapper,
                                  ContextoSeguridad contextoSeguridad,
                                  Caso_Repository casoRepository) {
        super(subtipoRepository, contextoSeguridad, Set.of("RAÍZ"), "Subtipo Caso",
                SubtipoCasoRequestDTO::nombre, subtipoCasoMapper::toEntity,
                subtipoCasoMapper::toResponse, subtipoCasoMapper::toResponseList,
                Subtipo_Caso::getNombre, Subtipo_Caso::setNombre, Subtipo_Caso::getId,
                subtipoRepository::findByNombreIgnoreCase,
                subtipoRepository::buscarPorNombreParcial,
                subtipoRepository::buscarPorNombreParcial);
        this.subtipoRepository = subtipoRepository;
        this.tipoRepository = tipoRepository;
        this.subtipoCasoMapper = subtipoCasoMapper;
        this.casoRepository = casoRepository;
    }

    /**
     * H1/A18 — El Tipo_Caso se carga ANTES de la comprobación de duplicado
     * (el orden anterior comprobaba antes de cargar el tipo) y se devuelve
     * como contexto para {@link #crearEntidad} y {@link #aplicarCambios}.
     */
    @Override
    protected Object preparar(SubtipoCasoRequestDTO request) {
        return tipoRepository.findById(request.tipoCaso())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de Caso no encontrado"));
    }

    /**
     * H1 — Unicidad por (tipoCaso, nombre), no global: V1 define
     * UNIQUE(id_tipo_caso, nombre). Dos tipos distintos pueden compartir
     * nombre de subtipo.
     */
    @Override
    protected void validarDuplicado(String nombreNormalizado, Integer excluirId, SubtipoCasoRequestDTO request) {
        subtipoRepository.findByTipoCasoIdAndNombre(request.tipoCaso(), nombreNormalizado)
                .filter(otro -> excluirId == null || !excluirId.equals(otro.getId()))
                .ifPresent(otro -> {
                    throw new IllegalStateException("Ya existe un subtipo con el nombre '" + nombreNormalizado + "'.");
                });
    }

    @Override
    protected boolean existeDuplicadoAhora(String nombreNormalizado, SubtipoCasoRequestDTO request) {
        return subtipoRepository.findByTipoCasoIdAndNombre(request.tipoCaso(), nombreNormalizado).isPresent();
    }

    /**
     * M27/B6 — Se usa el mapper (toEntity + setTipoCaso) en vez de construir
     * la entidad a mano con new + setters.
     */
    @Override
    protected Subtipo_Caso crearEntidad(SubtipoCasoRequestDTO request, String nombreNormalizado, Object contexto) {
        Subtipo_Caso subtipo = subtipoCasoMapper.toEntity(request);
        subtipo.setNombre(nombreNormalizado);
        subtipo.setTipoCaso((Tipo_Caso) contexto);
        return subtipo;
    }

    @Override
    protected void aplicarCambios(Subtipo_Caso entidad, SubtipoCasoRequestDTO request,
                                  String nombreNormalizado, Object contexto) {
        Tipo_Caso nuevoTipo = (Tipo_Caso) contexto;
        // M17 — Prohibido cambiar el tipo de un subtipo que ya tiene casos.
        if (!entidad.getTipoCaso().getId().equals(nuevoTipo.getId())
                && casoRepository.existsBySubtipo_Caso_Id(entidad.getId())) {
            throw new IllegalStateException("No se puede cambiar el tipo de un subtipo con casos asociados.");
        }
        entidad.setNombre(nombreNormalizado);
        entidad.setTipoCaso(nuevoTipo);
    }

    @Override
    protected boolean esVisible(Subtipo_Caso entidad) {
        // M16: el subtipo raíz del sistema no se lista ni se resuelve por id.
        return !"RAÍZ".equalsIgnoreCase(entidad.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtipoCasoResponseDTO> listarPorTipo(Integer idTipoCaso) {
        return listarPorTipo(idTipoCaso, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtipoCasoResponseDTO> listarPorTipo(Integer idTipoCaso, int page, int size) {
        return subtipoRepository.findByTipoCasoId(idTipoCaso, Paginacion.crear(page, size))
                .stream().filter(this::esVisible).map(subtipoCasoMapper::toResponse).toList();
    }

    @Override
    public List<SubtipoCasoResponseDTO> listarTodo(int page, int size) {
        return super.listarTodo(page, size);
    }
}
