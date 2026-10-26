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
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.AbogadoService;
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
    private final CurrentUser currentUser;

    // Constructor para inyección (más limpio que @Autowired en campos)
    public AbogadoServiceImpl(Abogado_Repository abogadoRepository,
                              Persona_Repository personaRepository,
                              Firma_Repository firmaRepository,
                              TipoAbogado_Repository tipoAbogadoRepository,
                              AbogadoMapper abogadoMapper,
                              CurrentUser currentUser) {
        this.abogadoRepository = abogadoRepository;
        this.personaRepository = personaRepository;
        this.firmaRepository = firmaRepository;
        this.tipoAbogadoRepository = tipoAbogadoRepository;
        this.abogadoMapper = abogadoMapper;
        this.currentUser = currentUser;
    }

    @Override
    public AbogadoResponseDTO guardar(AbogadoRequestDTO request) {
        // P0-B3: el alta de miembros es solo-admin (el controlador también lo exige).
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        // 1. Convertir DTO a Entidad base
        Abogado abogado = abogadoMapper.toEntity(request);

        // 2. Cargar relaciones físicas desde la BD
        abogado.setPersona(personaRepository.findById(request.idPersona())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada")));
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
        // P0-B2: firma del token (admin puede pedir otra firma).
        firmaId = currentUser.isAdmin() && firmaId != null ? firmaId : currentUser.requireFirmaId();
        // Usamos el repositorio que creamos antes para filtrar por firma y activos
        List<Abogado> abogados = abogadoRepository.findByAbogadosByFirmaActivos(firmaId);

        // El mapper se encarga de convertir toda la lista de un golpe
        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarTipo(Integer idFirma, String especialidad) {
        idFirma = currentUser.isAdmin() && idFirma != null ? idFirma : currentUser.requireFirmaId();
        List<Abogado> abogados = abogadoRepository.findByFirmaIdAndTipoAbogadoNombreIgnoreCase(idFirma, especialidad);

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public List<AbogadoResponseDTO> listarNombre(Integer idFirma, String nombre) {
        idFirma = currentUser.isAdmin() && idFirma != null ? idFirma : currentUser.requireFirmaId();
        List<Abogado> abogados = abogadoRepository.findByAbogadoByNombre(idFirma, nombre);

        return abogadoMapper.toResponseList(abogados);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorId(Integer id) {
        return abogadoRepository.findById(id)
                .map(abogado -> {
                    if (abogado.getFirma() != null) {
                        currentUser.requireFirma(abogado.getFirma().getId());
                    }
                    return abogadoMapper.toResponse(abogado);
                });
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorCc(String cc, Integer idFirma) {
        idFirma = currentUser.isAdmin() && idFirma != null ? idFirma : currentUser.requireFirmaId();
        return  abogadoRepository.findByAbogadoActivobyCc(cc, idFirma)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public Optional<AbogadoResponseDTO> buscarPorPersonaId(Integer id) {
        // P0-B2: uno mismo, admin, o misma firma del token.
        if (!currentUser.isAdmin() && !currentUser.personaId().equals(id)) {
            Integer firmaObjetivo = currentUser.firmaOfPersona(id);
            if (firmaObjetivo == null || !currentUser.canAccessFirma(firmaObjetivo)) {
                throw new SecurityException("No tiene permisos para realizar esta acción");
            }
        }
        return  abogadoRepository.findByPersonaId(id)
                .map(abogadoMapper::toResponse);
    }

    @Override
    public AbogadoResponseDTO actualizar(Integer id, AbogadoRequestDTO request){
        // P0-B3: modificar abogados (incl. cambiar de firma) es solo-admin.
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        return abogadoRepository.findById(id).map(oldAbogado -> {
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
        if (!currentUser.isAdmin()) {
            throw new SecurityException("No tiene permisos para realizar esta acción");
        }
        // 1. Buscamos al abogado
        Abogado abogado = abogadoRepository.findById(idAbogado)
                .orElseThrow(() -> new EntityNotFoundException("Abogado no encontrado"));

        // 2. Obtenemos su entidad Persona
        Persona persona = abogado.getPersona();

        // 3. Cambiamos el estado a false
        persona.setActivo(false);

        // 4. Guardamos la persona (esto desactiva al abogado en todo el sistema)
        personaRepository.save(persona);
    }
}
