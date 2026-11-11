package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.FirmaMapper;
import com.example.lexcloud.dto.request.FirmaRequestDTO;
import com.example.lexcloud.dto.response.FirmaResponseDTO;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.security.service.ContextoSeguridad;
import com.example.lexcloud.service.FirmaService;
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
public class FirmaServiceImpl implements FirmaService {

    private final Firma_Repository firmaRepository;
    private final FirmaMapper firmaMapper;
    private final ContextoSeguridad contextoSeguridad;

    public FirmaServiceImpl(Firma_Repository firmaRepository, FirmaMapper firmaMapper,
                            ContextoSeguridad contextoSeguridad) {
        this.firmaRepository = firmaRepository;
        this.firmaMapper = firmaMapper;
        this.contextoSeguridad = contextoSeguridad;
    }

    /** C3: las escrituras de firmas son solo para admin; las lecturas quedan abiertas. */
    private void exigirAdmin() {
        if (!contextoSeguridad.esAdmin()) {
            throw new SecurityException("Solo un administrador puede modificar las firmas.");
        }
    }

    @Override
    @Transactional
    public FirmaResponseDTO guardar(FirmaRequestDTO request) {
        Validaciones.requerirNoNulo(request, "request");
        String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre").trim();
        String nit = Validaciones.requerirNoBlank(request.nit(), "nit").trim();
        Validaciones.requerirNoNulo(request.integrante(), "integrante");
        exigirAdmin();
        // B2: el mensaje no reproduce el NIT (es el identificador que el
        // oráculo M28 protege: confirmar su existencia ya es filtrar).
        if (firmaRepository.findByNit(nit).isPresent()) {
            throw new IllegalStateException("Ya existe una firma con ese NIT.");
        }

        Firma firma = firmaMapper.toEntity(request);
        firma.setNombre(nombre);
        firma.setNit(nit);
        try {
            return firmaMapper.toResponse(firmaRepository.save(firma));
        } catch (DataIntegrityViolationException ex) {
            // El mensaje de NIT duplicado solo aplica si el NIT existe de verdad;
            // cualquier otra violación de integridad no debe reportarse como tal.
            if (firmaRepository.findByNit(nit).isPresent()) {
                throw new IllegalStateException("Ya existe una firma con ese NIT.");
            }
            throw new IllegalStateException("No se pudo guardar la firma. Verifique los datos e intente nuevamente.", ex);
        }
    }

    @Override
    public List<FirmaResponseDTO> listarTodas(int page, int size) {
        // P0-B2: listado base para el selector de firma del front. Solo
        // admin. La entidad Firma no tiene flag de activo, así que "activas"
        // son todas las firmas registradas.
        exigirAdmin();
        return firmaMapper.toResponseList(
                firmaRepository.findAll(Paginacion.crear(page, size)).getContent());
    }

    @Override
    public List<FirmaResponseDTO> listarPorNombre(String nombre) {
        return listarPorNombre(nombre, 0, Paginacion.TAMANO_POR_DEFECTO);
    }

    @Override
    public List<FirmaResponseDTO> listarPorNombre(String nombre, int page, int size) {
        String termino = nombre == null ? "" : nombre.trim();
        return firmaMapper.toResponseList(
                firmaRepository.findAllByNombre(termino, Paginacion.crear(page, size)));
    }

    /**
     * M28 — oráculo de existencia por NIT, cerrado: la consulta exige sesión
     * autenticada (sin ella, {@code idPersonaAutenticada()} lanza 403) y solo
     * devuelve la propia firma del llamador. Un NIT inexistente, de otra
     * firma, o consultado por un usuario sin firma resoluble responde vacío:
     * los tres casos son indistinguibles de "no existe", así que la respuesta
     * no filtra si un NIT existe. Decisión documentada: no se restringe a
     * admin (un abogado necesita validar el NIT de su propia firma).
     * {@code buscarPorId} se cerró aparte (S7): solo la propia firma,
     * salvo admin.
     */
    @Override
    public Optional<FirmaResponseDTO> buscarPorNit(String nit) {
        Integer firmaPropia = contextoSeguridad.idFirmaAutenticada().orElse(null);
        if (nit == null || firmaPropia == null) {
            return Optional.empty();
        }
        return firmaRepository.findByNit(nit.trim())
                .filter(firma -> firmaPropia.equals(firma.getId()))
                .map(firmaMapper::toResponse);
    }

    @Override
    public Optional<FirmaResponseDTO> buscarPorId(Integer id) {
        // S7: filtro de firma (antes abierto a cualquier autenticado). El
        // admin ve cualquier firma; un no-admin solo la suya; sin firma
        // resoluble no se devuelve nada (indistinguible de "no existe").
        if (contextoSeguridad.esAdmin()) {
            return firmaRepository.findById(id)
                    .map(firmaMapper::toResponse);
        }
        Integer firmaPropia = contextoSeguridad.idFirmaAutenticada().orElse(null);
        if (firmaPropia == null || !firmaPropia.equals(id)) {
            return Optional.empty();
        }
        return firmaRepository.findById(id)
                .map(firmaMapper::toResponse);
    }

    @Override
    @Transactional
    public FirmaResponseDTO actualizar(Integer id, FirmaRequestDTO request) {
        Validaciones.requerirId(id, "id");
        Validaciones.requerirNoNulo(request, "request");
        String nombre = Validaciones.requerirNoBlank(request.nombre(), "nombre").trim();
        String nit = Validaciones.requerirNoBlank(request.nit(), "nit").trim();
        exigirAdmin();
        Firma firma = firmaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada"));

        // El NIT es inmutable: identifica la firma en las carpetas de
        // documentos y en la auditoría, y antes se ignoraba en silencio (el
        // código comentado lo dejaba sin efecto). Un NIT distinto se rechaza
        // con 400 en vez de ignorarse.
        if (!firma.getNit().equals(nit)) {
            throw new IllegalArgumentException("El NIT de la firma no se puede cambiar.");
        }
        // Veto firma sistema por NIT (nunca por id): no se puede renombrar.
        // (No existe eliminar() en este servicio, así que el veto de borrado
        // no tiene dónde aplicarse; si se añade, debe usar esFirmaSistema.)
        if (contextoSeguridad.esFirmaSistema(firma) && !firma.getNombre().equals(nombre)) {
            throw new IllegalStateException("La firma del sistema no se puede renombrar.");
        }

        // Actualizamos los datos básicos
        firma.setNombre(nombre);
        // Solo se sobrescribe el número de integrantes cuando viene con valor
        if (request.integrante() != null) {
            firma.setIntegrante(request.integrante());
        }

        try {
            return firmaMapper.toResponse(firmaRepository.save(firma));
        } catch (DataIntegrityViolationException ex) {
            // El NIT es inmutable y único, así que un duplicado aquí solo
            // podría venir de una carrera; se distingue igual que en guardar.
            if (firmaRepository.findByNit(nit)
                    .filter(otra -> !otra.getId().equals(id)).isPresent()) {
                throw new IllegalStateException("Ya existe una firma con ese NIT.");
            }
            throw new IllegalStateException("No se pudo actualizar la firma. Verifique los datos e intente nuevamente.", ex);
        }
    }
}
