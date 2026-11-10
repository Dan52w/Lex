package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.AbogadoMapper;
import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;
import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Tipo_Abogado;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.TipoAbogado_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.AbogadoService;
import com.example.lexcloud.util.BusquedaUtil;
import com.example.lexcloud.util.Paginacion;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AbogadoServiceImpl implements AbogadoService {

    private final Abogado_Repository abogadoRepository;
    private final Persona_Repository personaRepository;
    private final Firma_Repository firmaRepository;
    private final TipoAbogado_Repository tipoAbogadoRepository;
    private final AbogadoMapper abogadoMapper;
    private final ContextoSeguridad contextoSeguridad;

    // Constructor para inyección (más limpio que @Autowired en campos)
    public AbogadoServiceImpl(Abogado_Repository abogadoRepository,
                              Persona_Repository personaRepository,
                              Firma_Repository firmaRepository,
                              TipoAbogado_Repository tipoAbogadoRepository,
                              AbogadoMapper abogadoMapper,
                              ContextoSeguridad contextoSeguridad) {
        this.abogadoRepository = abogadoRepository;
        this.personaRepository = personaRepository;
        this.firmaRepository = firmaRepository;
        this.tipoAbogadoRepository = tipoAbogadoRepository;
        this.abogadoMapper = abogadoMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    @Override
    public AbogadoResponseDTO guardar(AbogadoRequestDTO request) {
        contextoSeguridad.verificarMismaFirma(request.idFirma());

        // 1. Convertir DTO a Entidad base
        Abogado abogado = abogadoMapper.toEntity(request);

        // 2. Cargar relaciones físicas desde la BD
        Persona persona = personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada"));
        if (Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException("No se puede crear un abogado para una persona inactiva.");
        }
        if ("0000000000".equals(persona.getCc())) {
            throw new IllegalArgumentException("La persona del sistema no puede tener registro de abogado.");
        }
        abogado.setPersona(persona);
        // Sin duplicados: una persona solo puede tener un registro de abogado
        // (la BD tiene UNIQUE en id_persona).
        if (abogadoRepository.findByPersonaId(request.idPersona()).isPresent()) {
            throw new IllegalStateException("La persona ya tiene un registro de abogado.");
        }
        // ... setear firma y tipo abogado ...
        abogado.setFirma(firmaRepository.findById(request.idFirma())
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada")));
        abogado.setTipo_Abogado(tipoAbogadoRepository.findById(request.idTipoAbogado())
                .orElseThrow(() -> new EntityNotFoundException("Especialización no encontrada")));

        // 3. Guardar y retornar el ResponseDTO
        return abogadoMapper.toResponse(abogadoRepository.save(abogado));
    }

    @Override
    public List<AbogadoResponseDTO> listarTodos(Integer firmaId) {
        return listarTodos(firmaId, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AbogadoResponseDTO> listarTodos(Integer firmaId, int page, int size) {
        contextoSeguridad.verificarMismaFirma(firmaId);
        // Usamos el repositorio que creamos antes para filtrar por firma y activos
        List<Abogado> abogados = abogadoRepository.findByAbogadosByFirmaActivos(firmaId, Paginacion.crear(page, size));

        // El mapper se encarga de convertir toda la lista de un golpe
        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        // Sin variante paginada en el repositorio para esta consulta: la lista
        // viene sin límite (un firma no suele tener miles de abogados por
        // especialidad, pero es un residual de SVC-6).
        List<Abogado> abogados = abogadoRepository.findByFirmaIdAndTipoAbogadoNombreIgnoreCase(idFirma, especialidad);

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre) {
        return listarNombre(idFirma, nombre, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre, int page, int size) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        // SVC-5: el nombre se escapa antes del LIKE (ver BusquedaUtil).
        List<Abogado> abogados = abogadoRepository.findByAbogadoByNombre(
                idFirma, BusquedaUtil.escaparLike(nombre), Paginacion.crear(page, size));

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorId(Integer id) {
        return abogadoRepository.findById(id)
                .filter(this::perteneceAFirmaAutenticada)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorCc(String cc, Integer idFirma) {
        contextoSeguridad.verificarMismaFirma(idFirma);
        return  abogadoRepository.findByAbogadoActivobyCc(cc, idFirma)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorPersonaId(Integer id) {
        return  abogadoRepository.findByPersonaId(id)
                .filter(this::perteneceAFirmaAutenticada)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public AbogadoResponseDTO actualizar(Integer id, AbogadoRequestDTO request){
        return abogadoRepository.findById(id).map(oldAbogado -> {
            // Solo se puede modificar un abogado de la propia firma, y no se
            // puede mover a otra firma.
            contextoSeguridad.verificarAccesoAAbogado(oldAbogado);
            contextoSeguridad.verificarMismaFirma(request.idFirma());

            // 1. Buscamos las nuevas entidades relacionadas por su ID
            Firma nuevaFirma = firmaRepository.findById(request.idFirma())
                    .orElseThrow(() -> new EntityNotFoundException("La firma con ID " + request.idFirma() + " no existe"));
            Tipo_Abogado nuevoTipo = tipoAbogadoRepository.findById(request.idTipoAbogado())
                    .orElseThrow(() -> new EntityNotFoundException("El tipo de abogado con ID " + request.idTipoAbogado() + " no existe"));

            // 2. Actualizamos los campos de la entidad persistida
            oldAbogado.setFirma(nuevaFirma);
            oldAbogado.setTipo_Abogado(nuevoTipo);
            oldAbogado.setTarjeta_Profesional(request.tarjeta_Profesional());

            // Nota: Generalmente el ID de Persona no se cambia en un "Update Abogado"

            // 3. Guardamos y mapeamos a respuesta
            return abogadoMapper.toResponse(abogadoRepository.save(oldAbogado));

        }).orElseThrow(() -> new EntityNotFoundException("No se encontró el abogado con ID: " + id));
    }

    @Override
    public void desactivarAbogado(Integer idAbogado) {
        // 1. Buscamos al abogado
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));

        contextoSeguridad.verificarAccesoAAbogado(abogado);

        // 2. Obtenemos su entidad Persona
        Persona persona = abogado.getPersona();

        // 3. Cambiamos el estado a false
        persona.setActivo(false);

        // 4. Guardamos la persona (esto desactiva al abogado en todo el sistema)
        personaRepository.save(persona);
    }

    /**
     * S2: un abogado de otra firma no existe para quien consulta (se devuelve
     * vacío, como hace ClienteService.buscarPorId). Si la firma del usuario no
     * se puede resolver, no se puede verificar y se permite la consulta.
     */
    private boolean perteneceAFirmaAutenticada(Abogado abogado) {
        return contextoSeguridad.idFirmaAutenticada()
                .map(firmaId -> abogado.getFirma() != null
                        && firmaId.equals(abogado.getFirma().getId()))
                .orElse(true);
    }
}
