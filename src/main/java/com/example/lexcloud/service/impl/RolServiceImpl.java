package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.RolMapper;
import com.example.lexcloud.dto.request.RolRequestDTO;
import com.example.lexcloud.dto.response.RolResponseDTO;
import com.example.lexcloud.entidades.Rol;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.RolService;
import com.example.lexcloud.util.Paginacion;
import com.example.lexcloud.util.Validaciones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final Rol_Repository rolRepository;
    private final RolMapper rolMapper;
    private final ContextoSeguridad contextoSeguridad;

    public RolServiceImpl(Rol_Repository rolRepository, RolMapper rolMapper, ContextoSeguridad contextoSeguridad) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    /** C3: las escrituras de roles son solo para admin; las lecturas quedan abiertas. */
    private void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede modificar los roles.");
        }
    }

    /** W5: el rol 'SISTEMA' se reconoce por nombre normalizado, nunca por id. */
    private static boolean esRolSistema(Rol rol) {
        return rol.getNombre() != null && "SISTEMA".equalsIgnoreCase(rol.getNombre().trim());
    }

    @Override
    @Transactional
    public RolResponseDTO guardar(RolRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre").trim();
        exigirAdmin();
        if (rolRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new IllegalStateException("El rol con el nombre '" + nombre + "' ya existe.");
        }

        Rol rol = rolMapper.toEntity(request);
        rol.setNombre(nombre);
        // Los roles nuevos nacen con es_admin=false: el DTO no expone el flag
        // y el mapper lo deja en su valor por defecto, pero se fuerza aquí
        // para que ningún cambio futuro del mapper pueda crear un admin por
        // descuido. Solo una migración o un admin por BD puede dar es_admin.
        rol.setEsAdmin(false);
        try {
            return rolMapper.toResponse(rolRepository.save(rol));
        } catch (DataIntegrityViolationException ex) {
            // A16: re-consultar para distinguir el duplicado real (carrera)
            // de cualquier otra violación de integridad, que se re-lanza con
            // la causa encadenada en vez de tragársela.
            if (rolRepository.findByNombreIgnoreCase(nombre).isPresent()) {
                throw new IllegalStateException("El rol con el nombre '" + nombre + "' ya existe.");
            }
            throw new IllegalStateException("No se pudo guardar el rol.", ex);
        }
    }

    @Override
    public List<RolResponseDTO> listarTodo() {
        return listarTodo(0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<RolResponseDTO> listarTodo(int page, int size) {
        return rolMapper.toResponseList(
                rolRepository.findAll(Paginacion.crear(page, size)).getContent());
    }

    @Override
    public List<RolResponseDTO> listarPorNombre(String nombre) {
        return listarPorNombre(nombre, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<RolResponseDTO> listarPorNombre(String nombre, int page, int size) {
        String termino = nombre == null ? "" : nombre.trim();
        return rolMapper.toResponseList(
                rolRepository.findAllByNombre(termino, Paginacion.crear(page, size)));
    }

    @Override
    public Optional<RolResponseDTO> buscarPorNombre(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return rolRepository.findByNombreIgnoreCase(nombre.trim())
                .map(rolMapper::toResponse);
    }

    @Override
    public Optional<RolResponseDTO> buscarPorId(Integer id) {
        return rolRepository.findById(id)
                .map(rolMapper::toResponse);
    }

    @Override
    @Transactional
    public RolResponseDTO actualizar(Integer id, RolRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre").trim();
        exigirAdmin();
        Rol rolExistente = rolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado"));
        // El veto solo aplica si el nombre cambia de verdad: actualizar con el
        // mismo nombre es un no-op y no debe bloquearse.
        boolean cambiaNombre = rolExistente.getNombre() == null
                || !rolExistente.getNombre().trim().equalsIgnoreCase(nombre);
        if (cambiaNombre) {
            // Veto por flag (no por nombre): renombrar un rol admin rompería
            // el acceso de todos sus usuarios; el nombre es mutable y frágil.
            if (Boolean.TRUE.equals(rolExistente.getEsAdmin())) {
                throw new IllegalStateException("No se puede renombrar un rol administrador.");
            }
            // W5 (cierre del veto de borrado): 'SISTEMA' tampoco se puede renombrar.
            if (esRolSistema(rolExistente)) {
                throw new IllegalStateException("El rol 'SISTEMA' es del sistema y no se puede renombrar.");
            }
        }
        // Validar si el nuevo nombre ya lo tiene otro rol (case-insensitive).
        rolRepository.findByNombreIgnoreCase(nombre)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalStateException("El rol con el nombre '" + nombre + "' ya existe.");
                });

        rolExistente.setNombre(nombre);

        try {
            return rolMapper.toResponse(rolRepository.save(rolExistente));
        } catch (DataIntegrityViolationException ex) {
            // A16: mismo patrón que en guardar.
            if (rolRepository.findByNombreIgnoreCase(nombre)
                    .filter(otro -> !otro.getId().equals(id)).isPresent()) {
                throw new IllegalStateException("El rol con el nombre '" + nombre + "' ya existe.");
            }
            throw new IllegalStateException("No se pudo actualizar el rol.", ex);
        }
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Validaciones.requerirId(id, "id");
        exigirAdmin();
        // A15: cargar la entidad (404 si no existe) y borrar con delete(entidad),
        // no deleteById (evita el TOCTOU con EmptyResultDataAccessException).
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado"));
        // Veto por flag: borrar un rol admin dejaría a sus usuarios sin rol.
        if (Boolean.TRUE.equals(rol.getEsAdmin())) {
            throw new IllegalStateException("No se puede eliminar un rol administrador.");
        }
        // W5: 'SISTEMA' es el rol de la persona sistema (seed V1); borrarlo rompería el usuario sistema.
        if (esRolSistema(rol)) {
            throw new IllegalStateException("No se puede eliminar el rol 'SISTEMA': es el rol de la persona sistema.");
        }
        try {
            rolRepository.delete(rol);
            // A15: flush dentro del try para que la violación FK aflore aquí
            // (sin flush, saltaría en el commit, fuera del catch).
            rolRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // A15: 409 veraz con la causa encadenada (el rol está en uso).
            throw new IllegalStateException("No se puede eliminar: el rol está en uso.", ex);
        }
    }
}
