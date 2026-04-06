package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.mappers.ClienteMapper;
import com.example.lexcloud.dto.request.ClienteRequestDTO;
import com.example.lexcloud.dto.response.ClienteResponseDTO;
import com.example.lexcloud.entidades.Cliente;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Rol;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.service.ClienteService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {
    private final Cliente_Repository clienteRepository;
    private final Persona_Repository personaRepository;
    private final Firma_Repository firmaRepository;
    private final Rol_Repository rolRepository;
    private final ClienteMapper clienteMapper;

    public ClienteServiceImpl(Cliente_Repository clienteRepository,
                              Persona_Repository personaRepository,
                              Firma_Repository firmaRepository,
                              Rol_Repository rolRepository,
                              ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
        this.firmaRepository = firmaRepository;
        this.rolRepository = rolRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public ClienteResponseDTO guardar(ClienteRequestDTO request) {
        // 1. Buscamos si la persona ya existe en el sistema (por CC)
        Persona personaExistente = personaRepository.findByCc(request.cc())
                .orElseGet(() -> {
                    // Si no existe, la creamos desde cero
                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setCc(request.cc());
                    nuevaPersona.setNombre(request.nombre());
                    nuevaPersona.setApellido(request.apellido());
                    nuevaPersona.setFechaNacimiento(request.fechaNacimiento());
                    nuevaPersona.setTelefono(request.telefono());
                    nuevaPersona.setActivo(true);

                    Rol rol = rolRepository.findByNombre("Cliente")
                            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado"));
                    nuevaPersona.setRol(rol);
                    return personaRepository.save(nuevaPersona);
                });

        // 2. Verificamos si esta persona ya es cliente de ESTA firma específica
        Optional<Cliente> clienteExistenteEnFirma = clienteRepository
                .findByCcAndFirma(personaExistente.getCc(), request.idFirma());

        if (clienteExistenteEnFirma.isPresent()) {
            throw new IllegalStateException("Esta persona ya está registrada como cliente en esta firma.");
        }

        // 3. Si llegamos aquí, la persona existe (o se creó) pero no está en esta firma.
        // Creamos el vínculo de Cliente
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setPersona(personaExistente);
        nuevoCliente.setFirma(firmaRepository.findById(request.idFirma())
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada")));

        // El mapper o el constructor pueden manejar el resto (activo, fecha_vinculacion)
        nuevoCliente.setActivo(true);
        nuevoCliente.setFechaVinculacion(LocalDate.now());

        return clienteMapper.toResponse(clienteRepository.save(nuevoCliente));
    }

    @Override
    public List<ClienteResponseDTO> listarFirma(Integer idFirma) {
        return clienteMapper.toResponseList(clienteRepository.findAllByFirmaId(idFirma));
    }

    @Override
    public List<ClienteResponseDTO> listarNombre(Integer idFirma, String nombre) {
        return clienteMapper.toResponseList(clienteRepository.findByNombreAndFirma(idFirma, nombre));
    }

    @Override
    public Optional<ClienteResponseDTO> buscarPorCc(String cc, Integer idFirma) {
        return clienteRepository.findByCcAndFirma(cc, idFirma)
                .map(clienteMapper::toResponse);
    }

    @Override
    public Optional<ClienteResponseDTO> buscarPorIdPersona(Integer idPersona) {
        return clienteRepository.findByPersonaId(idPersona)
                .map(clienteMapper::toResponse);
    }

    @Override
    public Optional<ClienteResponseDTO> buscarPorId(Integer id) {
        return clienteRepository.findById(id)
                .map(clienteMapper::toResponse);
    }

    @Override
    public ClienteResponseDTO actualizar(Integer id, ClienteRequestDTO request) {
        return clienteRepository.findById(id).map(oldCliente -> {
            // Actualizamos los datos de la Persona vinculada
            Persona p = oldCliente.getPersona();
            p.setNombre(request.nombre());
            p.setApellido(request.apellido());
            p.setTelefono(request.telefono());
            p.setFechaNacimiento(request.fechaNacimiento());

            personaRepository.save(p);

            return clienteMapper.toResponse(clienteRepository.save(oldCliente));
        }).orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
    }

    @Override
    public void desactivarCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // Soft delete: Desactivamos el vínculo con la firma,
        // pero no borramos a la Persona del sistema global.
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }
}