package com.example.lexcloud.security;

import com.example.lexcloud.dto.mappers.VinculoCasoMapper;
import com.example.lexcloud.dto.request.VinculoCasoRequestDTO;
import com.example.lexcloud.dto.response.VinculoCasoResponseDTO;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Cliente;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Vinculo;
import com.example.lexcloud.entidades.Vinculo_Caso;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.VinculoCaso_Repository;
import com.example.lexcloud.repositorios.Vinculo_Repository;
import com.example.lexcloud.service.impl.VinculoCasoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0-A4 prueba 8: nadie se auto-autoriza a un caso ni vincula fuera de su
 * firma. El flag autorizado del DTO se ignora (antes se forzaba a true).
 */
@ExtendWith(MockitoExtension.class)
class VinculoAuthorizationTest {

    @Mock VinculoCaso_Repository vinculoRepo;
    @Mock Persona_Repository personaRepo;
    @Mock Cliente_Repository clienteRepo;
    @Mock Caso_Repository casoRepo;
    @Mock Vinculo_Repository tipoVinculoRepo;
    @Mock VinculoCasoMapper mapper;
    @Mock CurrentUser currentUser;
    @Mock AuditoriaService auditoria;

    private static Firma firma(int id) {
        Firma f = new Firma();
        f.setId(id);
        return f;
    }

    private VinculoCasoServiceImpl servicio() {
        return new VinculoCasoServiceImpl(vinculoRepo, personaRepo, clienteRepo,
                casoRepo, tipoVinculoRepo, mapper, currentUser, auditoria);
    }

    private void escenarioFirma1() {
        Persona persona = new Persona();
        persona.setId(10);
        persona.setActivo(true);
        Cliente cliente = new Cliente();
        cliente.setId(20);
        cliente.setActivo(true);
        cliente.setFirma(firma(1));
        Caso caso = new Caso();
        caso.setId(30);
        caso.setFirma(firma(1));
        when(personaRepo.findById(10)).thenReturn(Optional.of(persona));
        when(clienteRepo.findById(20)).thenReturn(Optional.of(cliente));
        when(casoRepo.findById(30)).thenReturn(Optional.of(caso));
        when(tipoVinculoRepo.findById(5)).thenReturn(Optional.of(new Vinculo()));
        when(currentUser.firmaOfPersona(10)).thenReturn(1);
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.personaId()).thenReturn(99);
        when(mapper.toEntity(any())).thenAnswer(inv -> new Vinculo_Caso());
        when(mapper.toResponse(any())).thenAnswer(inv -> {
            Vinculo_Caso vc = inv.getArgument(0);
            return new VinculoCasoResponseDTO(1, 10, "P", 20, "C", "123",
                    30, "Caso", 5, "Principal", vc.getAutorizado());
        });
        when(vinculoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void vincularNacePendienteAunqueElDtoPidaAutorizado() {
        escenarioFirma1();
        VinculoCasoResponseDTO creado = servicio().vincular(
                new VinculoCasoRequestDTO(10, 20, 30, 5, true));
        assertFalse(creado.autorizado(),
                "P0-B4: el vínculo nace pendiente; el DTO no puede auto-autorizar");
        ArgumentCaptor<Vinculo_Caso> captor = ArgumentCaptor.forClass(Vinculo_Caso.class);
        verify(vinculoRepo).save(captor.capture());
        assertFalse(captor.getValue().getAutorizado());
    }

    @Test
    void vincularPersonaDeOtraFirmaEsRechazado() {
        // vincular() resuelve persona/cliente/caso/vínculo antes del control
        // de firma: se stubbean las cuatro búsquedas (todas se usan) y la
        // persona a vincular pertenece a otra firma (2) que la del caso (1).
        Persona persona = new Persona();
        persona.setId(10);
        persona.setActivo(true);
        Cliente cliente = new Cliente();
        cliente.setId(20);
        cliente.setActivo(true);
        cliente.setFirma(firma(1));
        Caso caso = new Caso();
        caso.setId(30);
        caso.setFirma(firma(1));
        when(personaRepo.findById(10)).thenReturn(Optional.of(persona));
        when(clienteRepo.findById(20)).thenReturn(Optional.of(cliente));
        when(casoRepo.findById(30)).thenReturn(Optional.of(caso));
        when(tipoVinculoRepo.findById(5)).thenReturn(Optional.of(new Vinculo()));
        when(currentUser.firmaOfPersona(10)).thenReturn(2);
        assertThrows(SecurityException.class, () -> servicio().vincular(
                new VinculoCasoRequestDTO(10, 20, 30, 5, false)));
        verify(vinculoRepo, never()).save(any());
    }

    @Test
    void autorizarAMismoEsRechazado() {
        Vinculo_Caso vc = new Vinculo_Caso();
        vc.setId(77);
        Caso caso = new Caso();
        caso.setId(30);
        caso.setFirma(firma(1));
        vc.setCaso(caso);
        Persona vinculada = new Persona();
        vinculada.setId(10);
        vc.setPersonaFirma(vinculada);
        when(vinculoRepo.findById(77)).thenReturn(Optional.of(vc));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.personaId()).thenReturn(10); // el mismo vinculado
        when(currentUser.canAccessFirma(1)).thenReturn(true);
        when(currentUser.canAccessCaso(30)).thenReturn(true);
        assertThrows(SecurityException.class, () -> servicio().autorizarAcceso(77));
        verify(vinculoRepo, never()).save(any());
    }
}
