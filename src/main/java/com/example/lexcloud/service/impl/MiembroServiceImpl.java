package com.example.lexcloud.service.impl;

import com.example.lexcloud.dto.request.AbogadoRequestDTO;
import com.example.lexcloud.dto.request.EquipoAbogadoRequestDTO;
import com.example.lexcloud.dto.request.MiembroAltaRequestDTO;
import com.example.lexcloud.dto.request.RegisterRequestDTO;
import com.example.lexcloud.dto.response.AbogadoResponseDTO;
import com.example.lexcloud.dto.response.MiembroAltaResponseDTO;
import com.example.lexcloud.dto.response.PersonaResponseDTO;
import com.example.lexcloud.security.AuditoriaService;
import com.example.lexcloud.security.CurrentUser;
import com.example.lexcloud.service.AbogadoService;
import com.example.lexcloud.service.AuthService;
import com.example.lexcloud.service.EquipoAbogadoService;
import com.example.lexcloud.service.MiembroService;
import com.example.lexcloud.service.PersonaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P1-F4: alta de miembro transaccional.
 * Una única transacción (@Transactional): persona + abogado + login +
 * asignación de equipo opcional. Cualquier fallo (cédula duplicada, correo
 * duplicado, firma/tipo inexistente, contraseña débil) hace rollback de
 * todo: no quedan personas sin login ni abogados sin equipo.
 * Solo-admin (lo exige también el controlador). La firma del token es la
 * que manda: un admin de firma no puede crear miembros en otra firma.
 */
@Service
public class MiembroServiceImpl implements MiembroService {

    private final PersonaService personaService;
    private final AbogadoService abogadoService;
    private final AuthService authService;
    private final EquipoAbogadoService equipoAbogadoService;
    private final CurrentUser currentUser;
    private final AuditoriaService auditoriaService;

    public MiembroServiceImpl(PersonaService personaService,
                              AbogadoService abogadoService,
                              AuthService authService,
                              EquipoAbogadoService equipoAbogadoService,
                              CurrentUser currentUser,
                              AuditoriaService auditoriaService) {
        this.personaService = personaService;
        this.abogadoService = abogadoService;
        this.authService = authService;
        this.equipoAbogadoService = equipoAbogadoService;
        this.currentUser = currentUser;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public MiembroAltaResponseDTO crearMiembroCompleto(MiembroAltaRequestDTO request) {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("Solo un administrador puede dar de alta miembros");
        }
        Integer firmaId = currentUser.isAdmin() && currentUser.firmaId() == null
                ? request.idFirma()
                : currentUser.requireFirmaId();
        PersonaResponseDTO persona = personaService.guardar(request.persona());
        AbogadoResponseDTO abogado = abogadoService.guardar(new AbogadoRequestDTO(
                persona.id(), firmaId, request.idTipoAbogado(), request.tarjetaProfesional()));
        authService.registrar(new RegisterRequestDTO(persona.id(), request.correo(), request.password()));
        if (request.idTipoRelacion() != null) {
            equipoAbogadoService.crearAsignacion(new EquipoAbogadoRequestDTO(
                    abogado.id(), persona.id(), request.idTipoRelacion(),
                    request.fechaAsignacion(), true));
        }
        auditoriaService.registrar(currentUser.personaId(), firmaId,
                "MIEMBRO_CREADO", "persona", persona.id(), null);
        return new MiembroAltaResponseDTO(persona.id(), abogado.id(), request.correo());
    }
}
