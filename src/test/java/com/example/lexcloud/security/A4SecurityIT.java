package com.example.lexcloud.security;

import com.example.lexcloud.service.FileStorageService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite A4 (P0-A4 del plan): las 12 pruebas de seguridad de salida de P0,
 * contra la API REAL (HTTP + JPA + filtros + cookies), en PostgreSQL de
 * Testcontainers, con datos 100% ficticios. Ver A4BaseIT para el diseño.
 *
 * Orden fijo por nombre (t01..t12): la prueba 11 deja un contador de
 * intentos fallidos en memoria para un usuario DEDICADO (nunca usado en
 * otra prueba), así que el orden no contamina a las demás.
 *
 * Matiz documentado (no es un hueco): GET /api/clientes/{id} de otra
 * firma devuelve 404 y no 403, porque buscarPorId filtra por firma del
 * token y devuelve vacío. No filtra datos: el cuerpo no contiene nada
 * del cliente B. PUT y DELETE del mismo cliente sí dan 403.
 *
 * Matiz de la prueba 10: el almacenamiento real (Supabase, HTTP externo)
 * se sustituye por un stub SOLO en esta clase (@MockitoBean). La
 * validación que se prueba (lista blanca, magic bytes, nombre UUID y no
 * usar el nombre original) es código real del controlador/servicio; lo
 * que no se prueba aquí es la llamada HTTP a Supabase. Sin el stub, una
 * subida válida fallaría con 500 contra la URL ficticia y no se podría
 * demostrar la neutralización del traversal.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class A4SecurityIT extends A4BaseIT {

    @MockitoBean
    FileStorageService fileStorageService;

    @BeforeAll
    void sembrarA4() {
        sembrarFixture();
    }

    private String cookiesA() {
        return login("a4-firma-a@ejemplo.test", PASSWORD);
    }

    private String cookiesB() {
        return login("a4-firma-b@ejemplo.test", PASSWORD);
    }

    // ---------- 1. Casos de otra firma ----------

    @Test
    void t01_usuarioA_noLeeEditaNiBorraCasosDeB() {
        var f = fixture();
        String cookies = cookiesA();
        Integer casoB = f.casoB().getId();

        ResponseEntity<String> get = get("/api/casos/" + casoB, cookies);
        assertEquals(HttpStatus.FORBIDDEN, get.getStatusCode());
        assertFalse(cuerpo(get).contains("Caso Ficticio B"));

        String casoJson = "{\"idPersonaCreador\":" + f.personaA().getId()
                + ",\"idCliente\":" + f.clienteB().getId()
                + ",\"titulo\":\"Caso Ficticio B modificado\",\"descripcion\":\"x\""
                + ",\"idEstado\":1,\"idSubtipo\":1,\"idFirma\":" + f.firmaB().getId() + "}";
        ResponseEntity<String> put = putJson("/api/casos/" + casoB, cookies, casoJson);
        assertEquals(HttpStatus.FORBIDDEN, put.getStatusCode());

        ResponseEntity<String> del = delete("/api/casos/" + casoB, cookies);
        assertEquals(HttpStatus.FORBIDDEN, del.getStatusCode());
        assertTrue(casoRepository.findById(casoB).orElseThrow().getActivo(),
                "El caso B no debe haberse desactivado por el intento de A");

        // Control positivo + regresión de la consulta JPQL corregida
        // (superBuscador): con el idPersona de B en la URL, el servicio
        // usa el token (A) y no debe devolver el caso B; si la JPQL
        // siguiera rota, esta llamada fallaría con 500 en vez de 200.
        ResponseEntity<String> mios = get("/api/casos/mis-casos", cookies);
        assertEquals(HttpStatus.OK, mios.getStatusCode());
        assertTrue(cuerpo(mios).contains("Caso Ficticio A"));
        assertFalse(cuerpo(mios).contains("Caso Ficticio B"));
        ResponseEntity<String> global = get("/api/casos/buscar/global?idPersona="
                + f.personaB().getId() + "&query=Ficticio", cookies);
        assertEquals(HttpStatus.OK, global.getStatusCode());
        assertFalse(cuerpo(global).contains("Caso Ficticio B"));
    }

    // ---------- 2. Clientes de otra firma ----------

    @Test
    void t02_usuarioA_noLeeEditaNiBorraClientesDeB() {
        var f = fixture();
        String cookies = cookiesA();
        Integer clienteB = f.clienteB().getId();

        // 404 por diseño (filtro por firma del token), sin datos de B.
        ResponseEntity<String> get = get("/api/clientes/" + clienteB, cookies);
        assertEquals(HttpStatus.NOT_FOUND, get.getStatusCode());
        assertFalse(cuerpo(get).contains("Cliente Ficticio B"));

        String clienteJson = "{\"identificacion\":\"900000002\",\"nombres\":\"Cliente Ficticio B\","
                + "\"primerApellido\":\"Ficticio\",\"fechaNacimiento\":\"1985-05-05\","
                + "\"telefonoMovil\":\"3000000001\",\"correo\":\"a4-cliente-b@ejemplo.test\","
                + "\"idFirma\":" + f.firmaB().getId() + ",\"idTipoCliente\":1}";
        ResponseEntity<String> put = putJson("/api/clientes/" + clienteB, cookies,
                clienteJson);
        assertEquals(HttpStatus.FORBIDDEN, put.getStatusCode());

        ResponseEntity<String> del = delete("/api/clientes/" + clienteB, cookies);
        assertEquals(HttpStatus.FORBIDDEN, del.getStatusCode());
        assertTrue(clienteRepository.findById(clienteB).orElseThrow().getActivo());

        ResponseEntity<String> mios = get("/api/clientes/mi-firma", cookies);
        assertEquals(HttpStatus.OK, mios.getStatusCode());
        assertFalse(cuerpo(mios).contains("Cliente Ficticio B"));
    }

    // ---------- 3. Documentos de otra firma ----------

    @Test
    void t03_usuarioA_noDescargaNiBorraDocumentosDeB() {
        var f = fixture();
        String cookies = cookiesA();
        Integer docB = f.documentoB().getId();

        assertEquals(HttpStatus.FORBIDDEN, get("/api/documentos/" + docB, cookies)
                .getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/documentos/descargar/" + docB, cookies).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, delete("/api/documentos/" + docB, cookies)
                .getStatusCode());
        // El intento no lo envió a papelera ni tocó Supabase (el 403 llega
        // antes del almacenamiento).
        assertTrue(documentoRepository.findById(docB).orElseThrow().getEliminadoEn()
                == null);
        Mockito.verifyNoInteractions(fileStorageService);
    }

    // ---------- 4. Agenda e historial de casos de otra firma ----------

    @Test
    void t04_usuarioA_noVeAgendaNiHistorialDeCasosDeB() {
        var f = fixture();
        String cookies = cookiesA();
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/agendas/" + f.agendaB().getId(), cookies).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/agendas/caso/" + f.casoB().getId(), cookies).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/historial-cambios/caso/" + f.casoB().getId(), cookies)
                        .getStatusCode());
    }

    // ---------- 5. Sin rol admin no crea roles/firmas/catálogos,
    // ni cambia roles ni da de alta miembros ----------

    @Test
    void t05_sinAdmin_noCreaRolesFirmasCatalogosNiMiembros() {
        var f = fixture();
        String cookies = cookiesA();
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/roles", cookies, "{\"nombre\":\"Rol Ficticio A4\"}")
                        .getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                putJson("/api/roles/" + f.rolAbogado().getId(), cookies,
                        "{\"nombre\":\"Rol Ficticio A4\"}").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/firmas", cookies,
                        "{\"nombre\":\"Firma Intrusa\",\"nit\":\"900000099-9\",\"integrante\":1}")
                        .getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/tipo-casos", cookies, "{\"nombre\":\"Tipo Ficticio A4\"}")
                        .getStatusCode());
        String miembroJson = "{\"persona\":{\"nombre\":\"Intruso\",\"apellido\":\"Ficticio\","
                + "\"cc\":\"100000099\",\"idRol\":" + f.rolAbogado().getId()
                + ",\"telefono\":\"3000000099\",\"fechaNacimiento\":\"1990-01-01\"},"
                + "\"idFirma\":" + f.firmaA().getId() + ",\"idTipoAbogado\":1,"
                + "\"tarjetaProfesional\":\"TP-INTRUSA\",\"correo\":\"a4-intruso@ejemplo.test\","
                + "\"password\":\"Contrasena-A4-123\"}";
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/miembros", cookies, miembroJson).getStatusCode());
    }

    // ---------- 6. No registrarse sobre el idPersona de otro ----------

    @Test
    void t06_autenticado_noSeRegistraSobreIdPersonaDeOtro() {
        var f = fixture();
        // El registro es solo-admin (@PreAuthorize): un no-admin recibe
        // 403 aunque el idPersona sea de otra persona, y sin autenticar
        // recibe 401. Ambas vías quedan cerradas.
        String body = "{\"idPersona\":" + f.personaB().getId()
                + ",\"correo\":\"a4-intruso-registro@ejemplo.test\","
                + "\"password\":\"Contrasena-A4-123\"}";
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/auth/register", cookiesA(), body).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                postJson("/api/auth/register", null, body).getStatusCode());
        assertFalse(loginRepository.existsByCorreo("a4-intruso-registro@ejemplo.test"));
    }

    // ---------- 7. No cambia su propio rol ni reactiva cuentas ----------

    @Test
    void t07_usuario_noCambiaSuRolNiReactivaCuentaDesactivada() {
        var f = fixture();
        String cookies = cookiesA();
        String personaJson = "{\"nombre\":\"Ana\",\"apellido\":\"Ficticia A\","
                + "\"cc\":\"100000001\",\"idRol\":" + f.rolAdmin().getId()
                + ",\"telefono\":\"3000000000\",\"fechaNacimiento\":\"1990-01-01\"}";
        assertEquals(HttpStatus.FORBIDDEN,
                putJson("/api/personas/" + f.personaA().getId(), cookies, personaJson)
                        .getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                patchSinBody("/api/personas/" + f.personaDesactivada().getId()
                        + "/reactivar", cookies).getStatusCode());
        assertFalse(personaRepository.findById(f.personaDesactivada().getId())
                .orElseThrow().getActivo());
        // Y su propio rol sigue siendo ABOGADO (puede seguir entrando como tal).
        assertEquals(HttpStatus.OK, get("/api/auth/me", cookies).getStatusCode());
        assertTrue(cuerpo(get("/api/auth/me", cookies)).contains("ABOGADO"));
    }

    // ---------- 8. No auto-autorizarse ni ver pendientes de otra firma ----------

    @Test
    void t08_usuario_noSeAutoAutorizaNiVePendientesDeOtraFirma() {
        var f = fixture();
        String cookies = cookiesA();
        Integer vinculoId = vinculoRepository.findByNombre("SIN VÍNCULO")
                .orElseThrow().getId();
        String vinculoJson = "{\"idPersona\":" + f.personaA().getId()
                + ",\"idCliente\":" + f.clienteB().getId()
                + ",\"idCaso\":" + f.casoB().getId()
                + ",\"idVinculo\":" + vinculoId + ",\"autorizado\":true}";
        assertEquals(HttpStatus.FORBIDDEN,
                postJson("/api/vinculo-casos", cookies, vinculoJson).getStatusCode());

        // Nadie se autoriza a sí mismo: el pendiente de A sigue pendiente.
        assertEquals(HttpStatus.FORBIDDEN,
                patchSinBody("/api/vinculo-casos/" + f.pendienteA().getId()
                        + "/autorizar", cookies).getStatusCode());
        assertFalse(vinculoCasoRepository.findById(f.pendienteA().getId())
                .orElseThrow().getAutorizado());

        // Pendientes: solo los de la firma del token.
        ResponseEntity<String> pendientes = get("/api/vinculo-casos/pendientes",
                cookies);
        assertEquals(HttpStatus.OK, pendientes.getStatusCode());
        assertTrue(cuerpo(pendientes).contains("\"id\":" + f.pendienteA().getId()));
        assertFalse(cuerpo(pendientes).contains("\"id\":" + f.pendienteB().getId()));
    }

    // ---------- 9. Token manipulado / revocado tras logout -> 401 ----------
    // (El subcaso "expirado" está en A4TokenExpiracionIT, con expiración
    // de 1 segundo: en esta clase el access dura 15 minutos y no se puede
    // esperar a que expire dentro del test.)

    @Test
    void t09_tokenManipuladoORevocado_devuelve401SinDatos() {
        String cookies = cookiesA();
        String access = valorCookie(cookies, "lex_access");
        String refresh = valorCookie(cookies, "lex_refresh");
        assertFalse(access.isBlank());
        assertFalse(refresh.isBlank());

        // Sin cookies -> 401.
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", null)
                .getStatusCode());
        // Access manipulado (último carácter cambiado) -> 401, sin datos.
        String manipulado = "lex_access=" + access.substring(0, access.length() - 1)
                + (access.endsWith("A") ? "B" : "A");
        ResponseEntity<String> resp = get("/api/auth/me", manipulado);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertFalse(cuerpo(resp).contains("a4-firma-a@ejemplo.test"));

        // Logout revoca el refresh: reutilizarlo da 401.
        ResponseEntity<String> logout = postJson("/api/auth/logout", cookies, null);
        assertEquals(HttpStatus.OK, logout.getStatusCode());
        ResponseEntity<String> refreshResp = postJson("/api/auth/refresh",
                "lex_refresh=" + refresh, null);
        assertEquals(HttpStatus.UNAUTHORIZED, refreshResp.getStatusCode());
    }

    // ---------- 10. Subidas peligrosas rechazadas o neutralizadas ----------

    @Test
    void t10_subidasPeligrosas_rechazadasONeutralizadas() {
        var f = fixture();
        String cookies = cookiesA();
        String datos = "{\"idCaso\":" + f.casoA().getId()
                + ",\"nombre\":\"Documento A4\",\"descripcion\":\"Prueba\","
                + "\"idTipoDocumento\":" + f.tipoDocumento().getId()
                + ",\"nitFirma\":\"900000001-1\"}";

        // Ejecutable -> 400 (fuera de la lista blanca).
        assertEquals(HttpStatus.BAD_REQUEST,
                subirDocumento(cookies, datos, "malware.exe",
                        "MZ".getBytes()).getStatusCode());
        // HTML con script -> 400 (fuera de la lista blanca).
        assertEquals(HttpStatus.BAD_REQUEST,
                subirDocumento(cookies, datos, "pagina.html",
                        "<script>alert(1)</script>".getBytes()).getStatusCode());

        // Nombre con traversal + contenido PDF válido: el nombre original
        // NUNCA se usa (el servidor guarda un UUID); el stub de
        // almacenamiento devuelve la ruta virtual y se comprueba que la
        // dirección guardada no contiene ".." ni el nombre original.
        Mockito.when(fileStorageService.guardarArchivo(Mockito.any(), Mockito.any(),
                        Mockito.any())).thenReturn("firma_A/a4-uuid-falso.pdf");
        ResponseEntity<String> ok = subirDocumento(cookies, datos,
                "../secreto.pdf", "%PDF-1.4 falso".getBytes());
        assertEquals(HttpStatus.CREATED, ok.getStatusCode());
        assertFalse(cuerpo(ok).contains(".."));
        assertFalse(cuerpo(ok).contains("secreto"));
    }

    // ---------- 11. Límite tras varios logins fallidos ----------

    @Test
    void t11_trasVariosLoginsFallidos_hayLimite429() {
        String correo = "a4-bloqueo@ejemplo.test";
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> fallo = postJson("/api/auth/login", null,
                    "{\"correo\":\"" + correo + "\",\"password\":\"ClaveIncorrecta-1\"}");
            assertEquals(HttpStatus.UNAUTHORIZED, fallo.getStatusCode());
        }
        // El sexto intento (incluso con la clave correcta) ya está bloqueado.
        ResponseEntity<String> bloqueado = postJson("/api/auth/login", null,
                "{\"correo\":\"" + correo + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, bloqueado.getStatusCode());
        // El bloqueo es por correo: otro usuario sigue entrando.
        assertEquals(HttpStatus.OK,
                postJson("/api/auth/login", null,
                        "{\"correo\":\"a4-firma-a@ejemplo.test\",\"password\":\""
                                + PASSWORD + "\"}").getStatusCode());
    }

    // ---------- 12. existe-correo sin autenticación no revela nada ----------

    @Test
    void t12_existeCorreoSinAutenticacion_noRevelaSiExiste() {
        String correo = "a4-firma-a@ejemplo.test";
        ResponseEntity<String> sinAuth = get("/api/auth/existe-correo/" + correo, null);
        assertEquals(HttpStatus.UNAUTHORIZED, sinAuth.getStatusCode());
        assertFalse(cuerpo(sinAuth).contains("true"));
        // Tampoco un no-admin: existe-correo es solo-admin.
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/auth/existe-correo/" + correo, cookiesA()).getStatusCode());
        // Y para un correo inexistente la respuesta sin auth es idéntica
        // (misma 401): no hay oráculo para enumerar correos.
        ResponseEntity<String> inexistente = get(
                "/api/auth/existe-correo/no-existe@ejemplo.test", null);
        assertEquals(HttpStatus.UNAUTHORIZED, inexistente.getStatusCode());
    }

    // ---------- Utilidades ----------

    private String cuerpo(ResponseEntity<String> resp) {
        return resp.getBody() == null ? "" : resp.getBody();
    }

    private String valorCookie(String cookies, String nombre) {
        for (String par : cookies.split("; ")) {
            if (par.startsWith(nombre + "=")) {
                return par.substring(nombre.length() + 1);
            }
        }
        return "";
    }

    private ResponseEntity<String> subirDocumento(String cookies, String datosJson,
                                                  String nombreArchivo,
                                                  byte[] contenido) {
        HttpHeaders datosHeaders = new HttpHeaders();
        datosHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> datosPart = new HttpEntity<>(datosJson, datosHeaders);
        ByteArrayResource archivoPart = new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombreArchivo;
            }
        };
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("datos", datosPart);
        partes.add("archivo", archivoPart);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (cookies != null && !cookies.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookies);
        }
        return rest.exchange(url("/api/documentos"), HttpMethod.POST,
                new HttpEntity<>(partes, headers), String.class);
    }
}
