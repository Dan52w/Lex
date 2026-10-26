package com.example.lexcloud.security;

import com.example.lexcloud.entidades.Abogado;
import com.example.lexcloud.entidades.Caso;
import com.example.lexcloud.entidades.Cliente;
import com.example.lexcloud.entidades.Firma;
import com.example.lexcloud.entidades.Login;
import com.example.lexcloud.entidades.Persona;
import com.example.lexcloud.entidades.Rol;
import com.example.lexcloud.entidades.Tipo_Abogado;
import com.example.lexcloud.entidades.Tipo_Cliente;
import com.example.lexcloud.repositorios.Abogado_Repository;
import com.example.lexcloud.repositorios.Agenda_Repository;
import com.example.lexcloud.repositorios.Caso_Repository;
import com.example.lexcloud.repositorios.Documento_Repository;
import com.example.lexcloud.repositorios.HistorialCambio_Repository;
import com.example.lexcloud.repositorios.Cliente_Repository;
import com.example.lexcloud.repositorios.Estado_Repository;
import com.example.lexcloud.repositorios.Firma_Repository;
import com.example.lexcloud.repositorios.Login_Repository;
import com.example.lexcloud.repositorios.Persona_Repository;
import com.example.lexcloud.repositorios.Rol_Repository;
import com.example.lexcloud.repositorios.SubtipoCaso_Repository;
import com.example.lexcloud.repositorios.TipoAbogado_Repository;
import com.example.lexcloud.repositorios.TipoCliente_Repository;
import com.example.lexcloud.repositorios.TipoDocumento_Repository;
import com.example.lexcloud.repositorios.TipoAgenda_Repository;
import com.example.lexcloud.repositorios.VinculoCaso_Repository;
import com.example.lexcloud.repositorios.Vinculo_Repository;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

/**
 * Base común de la suite A4 (P0-A4 del plan).
 *
 * - API REAL: @SpringBootTest con puerto aleatorio y TestRestTemplate
 *   contra los controladores, servicios, filtros y cookies reales
 *   (lex_access / lex_refresh). No hay MockMvc ni servicios mockeados,
 *   salvo el almacenamiento de archivos (ver A4SecurityIT, prueba 10):
 *   Supabase es un HTTP externo y en un test no debe tocarse.
 * - BASE DE TEST SEPARADA: un PostgreSQL efímero de Testcontainers, con
 *   las migraciones Flyway V1-V4 reales. NUNCA se toca db_Lex del
 *   usuario ni ningún dato real. Todos los datos sembrados son ficticios
 *   (correos .test, NIT y radicados inventados, nombres de ejemplo).
 * - Requisito en el PC de Daniel: Docker Desktop en marcha (Testcontainers
 *   levanta el contenedor solo). Sin Docker, esta suite no arranca: es
 *   el coste de probar contra PostgreSQL real y no contra H2.
 * - La siembra es UNA vez por clase (@BeforeAll + PER_CLASS): los
 *   intentos entre firmas se rechazan ANTES de mutar (requireCaso /
 *   @PreAuthorize), así que ningún test A4 altera la siembra de otro,
 *   salvo la subida válida de la prueba 10, que solo añade un documento
 *   al caso A y no afecta a ninguna otra aserción.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        // Secreto SOLO de test (>= 32 bytes para HS256). Nunca un secreto real.
        "lexcloud.jwt.secret=a4-solo-test-secreto-de-al-menos-32-bytes-0123456789",
        // Cookies usables por HTTP en el test (el test envía el header Cookie a mano).
        "lexcloud.cookie.secure=false",
        "lexcloud.cookie.samesite=Lax",
        // Supabase ficticio: ningún test A4 debe llegar a él (ver prueba 10).
        "supabase.storage.url=http://localhost:9/a4-test/",
        "supabase.storage.key=a4-test-dummy-key",
        "lexcloud.documentos.max-bytes=10485760"
})
abstract class A4BaseIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void propiedadesPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // El esquema lo gobierna Flyway (V1 se ejecuta completa en la BD
        // vacía del contenedor; en la BD existente del usuario se usaría
        // baseline-on-migrate=true, pero aquí la BD nace vacía).
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    int puerto;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    Rol_Repository rolRepository;
    @Autowired
    Firma_Repository firmaRepository;
    @Autowired
    Persona_Repository personaRepository;
    @Autowired
    Login_Repository loginRepository;
    @Autowired
    Abogado_Repository abogadoRepository;
    @Autowired
    Cliente_Repository clienteRepository;
    @Autowired
    Caso_Repository casoRepository;
    @Autowired
    Estado_Repository estadoRepository;
    @Autowired
    SubtipoCaso_Repository subtipoRepository;
    @Autowired
    TipoCliente_Repository tipoClienteRepository;
    @Autowired
    TipoDocumento_Repository tipoDocumentoRepository;
    @Autowired
    TipoAgenda_Repository tipoAgendaRepository;
    @Autowired
    TipoAbogado_Repository tipoAbogadoRepository;
    @Autowired
    Vinculo_Repository vinculoRepository;
    @Autowired
    VinculoCaso_Repository vinculoCasoRepository;
    @Autowired
    Documento_Repository documentoRepository;
    @Autowired
    Agenda_Repository agendaRepository;
    @Autowired
    HistorialCambio_Repository historialRepository;

    static final String PASSWORD = "Contrasena-A4-123";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** Siembra ficticia compartida. Devuelve los ids que usan los tests. */
    record Fixture(
            Rol rolAbogado, Rol rolAdmin,
            Firma firmaA, Firma firmaB,
            Persona personaA, Persona personaB, Persona personaB2,
            Persona personaBloqueo, Persona personaDesactivada,
            Abogado abogadoA, Abogado abogadoB,
            Cliente clienteA, Cliente clienteB,
            Caso casoA, Caso casoB,
            com.example.lexcloud.entidades.Documento documentoB,
            com.example.lexcloud.entidades.Agenda agendaB,
            com.example.lexcloud.entidades.Historial_Cambio historialB,
            com.example.lexcloud.entidades.Vinculo_Caso pendienteA,
            com.example.lexcloud.entidades.Vinculo_Caso pendienteB,
            com.example.lexcloud.entidades.Tipo_Documento tipoDocumento,
            com.example.lexcloud.entidades.Tipo_Agenda tipoAgenda,
            Caso casoRaiz) {
    }

    private Fixture fixture;

    Fixture fixture() {
        return fixture;
    }

    /**
     * Siembra completa. Cada subclase la llama desde su propio @BeforeAll:
     * si fuera @BeforeAll aquí, JUnit la ejecutaría también en las
     * subclases que tienen su propia siembra (como la de expiración).
     */
    void sembrarFixture() {
        Rol rolAbogado = new Rol();
        rolAbogado.setNombre("ABOGADO");
        rolAbogado = rolRepository.save(rolAbogado);
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMIN");
        rolAdmin = rolRepository.save(rolAdmin);

        Firma firmaA = nuevaFirma("Firma Ficticia A", "900000001-1");
        Firma firmaB = nuevaFirma("Firma Ficticia B", "900000002-2");

        Tipo_Abogado tipoAbogado = new Tipo_Abogado();
        tipoAbogado.setNombre("Litigante Ficticio");
        tipoAbogado = tipoAbogadoRepository.save(tipoAbogado);

        Persona personaA = nuevaPersona("Ana", "Ficticia A", "100000001", rolAbogado,
                "a4-firma-a@ejemplo.test");
        Persona personaB = nuevaPersona("Bruno", "Ficticio B", "100000002", rolAbogado,
                "a4-firma-b@ejemplo.test");
        Persona personaB2 = nuevaPersona("Beatriz", "Ficticia B2", "100000003", rolAbogado,
                "a4-firma-b2@ejemplo.test");
        Persona personaBloqueo = nuevaPersona("Bloqueo", "Ficticio", "100000004", rolAbogado,
                "a4-bloqueo@ejemplo.test");
        Persona personaDesactivada = nuevaPersona("Desactivada", "Ficticia", "100000005",
                rolAbogado, null);
        personaDesactivada.setActivo(false);
        personaDesactivada = personaRepository.save(personaDesactivada);

        Abogado abogadoA = nuevoAbogado(personaA, firmaA, tipoAbogado, "TP-FICTICIA-A");
        Abogado abogadoB = nuevoAbogado(personaB, firmaB, tipoAbogado, "TP-FICTICIA-B");

        Tipo_Cliente tipoCliente = tipoClienteRepository.findByNombre("Natural")
                .orElseThrow();
        Cliente clienteA = nuevoCliente(firmaA, tipoCliente, "900000001",
                "Cliente Ficticio A", "a4-cliente-a@ejemplo.test");
        Cliente clienteB = nuevoCliente(firmaB, tipoCliente, "900000002",
                "Cliente Ficticio B", "a4-cliente-b@ejemplo.test");

        Caso casoRaiz = casoRepository.findAll().stream()
                .filter(c -> "CASO MAESTRO SISTEMA".equals(c.getTitulo()))
                .findFirst().orElseThrow();
        com.example.lexcloud.entidades.Estado estado = estadoRepository
                .findByNombre("SIN DEFINIR").orElseThrow();
        com.example.lexcloud.entidades.Subtipo_Caso subtipo = subtipoRepository
                .findByNombre("RAÍZ").orElseThrow();
        com.example.lexcloud.entidades.Vinculo vinculo = vinculoRepository
                .findByNombre("SIN VÍNCULO").orElseThrow();

        Caso casoA = nuevoCaso(firmaA, estado, subtipo, casoRaiz,
                "Caso Ficticio A", 9000000001L);
        Caso casoB = nuevoCaso(firmaB, estado, subtipo, casoRaiz,
                "Caso Ficticio B", 9000000002L);

        nuevoVinculo(personaA, clienteA, casoA, vinculo, true);
        nuevoVinculo(personaB, clienteB, casoB, vinculo, true);
        com.example.lexcloud.entidades.Vinculo_Caso pendienteA =
                nuevoVinculo(personaBloqueo, clienteA, casoA, vinculo, false);
        com.example.lexcloud.entidades.Vinculo_Caso pendienteB =
                nuevoVinculo(personaB2, clienteB, casoB, vinculo, false);

        com.example.lexcloud.entidades.Tipo_Documento tipoDocumento =
                new com.example.lexcloud.entidades.Tipo_Documento();
        tipoDocumento.setNombre("Prueba Ficticia");
        tipoDocumento = tipoDocumentoRepository.save(tipoDocumento);

        com.example.lexcloud.entidades.Documento documentoB =
                new com.example.lexcloud.entidades.Documento();
        documentoB.setNombre("Documento Ficticio B");
        documentoB.setDescripcion("Documento de prueba, firma B");
        documentoB.setDireccion("firma_B/a4-documento-ficticio-b.pdf");
        documentoB.setTamanio(1234L);
        documentoB.setCaso(casoB);
        documentoB.setTipo_Documento(tipoDocumento);
        documentoB = documentoRepository.save(documentoB);

        com.example.lexcloud.entidades.Tipo_Agenda tipoAgenda =
                new com.example.lexcloud.entidades.Tipo_Agenda();
        tipoAgenda.setNombre("Audiencia Ficticia");
        tipoAgenda = tipoAgendaRepository.save(tipoAgenda);

        com.example.lexcloud.entidades.Agenda agendaB =
                new com.example.lexcloud.entidades.Agenda();
        agendaB.setActiva(true);
        agendaB.setDescripcion("Audiencia ficticia B");
        agendaB.setFechaExp(java.time.LocalDateTime.of(2099, 1, 1, 10, 0));
        agendaB.setAbogado(abogadoB);
        agendaB.setTipo_Agenda(tipoAgenda);
        agendaB.setCaso(casoB);
        agendaB = agendaRepository.save(agendaB);

        com.example.lexcloud.entidades.Historial_Cambio historialB =
                new com.example.lexcloud.entidades.Historial_Cambio();
        historialB.setFecha(LocalDate.now());
        historialB.setDescripcion("Hito ficticio B");
        historialB.setCaso(casoB);
        historialB.setPersona(personaB);
        historialB = historialRepository.save(historialB);

        fixture = new Fixture(rolAbogado, rolAdmin, firmaA, firmaB, personaA, personaB,
                personaB2, personaBloqueo, personaDesactivada, abogadoA, abogadoB,
                clienteA, clienteB, casoA, casoB, documentoB, agendaB, historialB,
                pendienteA, pendienteB, tipoDocumento, tipoAgenda, casoRaiz);
    }

    private Firma nuevaFirma(String nombre, String nit) {
        Firma firma = new Firma();
        firma.setNombre(nombre);
        firma.setNit(nit);
        firma.setIntegrante(1);
        return firmaRepository.save(firma);
    }

    private Persona nuevaPersona(String nombre, String apellido, String cc, Rol rol,
                                 String correo) {
        Persona persona = new Persona();
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCc(cc);
        persona.setRol(rol);
        persona.setTelefono("3000000000");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setActivo(true);
        persona = personaRepository.save(persona);
        if (correo != null) {
            Login login = new Login();
            login.setPersona(persona);
            login.setCorreo(correo);
            login.setPassword(ENCODER.encode(PASSWORD));
            loginRepository.save(login);
        }
        return persona;
    }

    private Abogado nuevoAbogado(Persona persona, Firma firma, Tipo_Abogado tipo,
                                 String tarjeta) {
        Abogado abogado = new Abogado();
        abogado.setPersona(persona);
        abogado.setFirma(firma);
        abogado.setTipo_Abogado(tipo);
        abogado.setTarjeta_Profesional(tarjeta);
        return abogadoRepository.save(abogado);
    }

    private Cliente nuevoCliente(Firma firma, Tipo_Cliente tipo, String identificacion,
                                 String nombres, String correo) {
        Cliente cliente = new Cliente();
        cliente.setFirma(firma);
        cliente.setTipoCliente(tipo);
        cliente.setIdentificacion(identificacion);
        cliente.setNombres(nombres);
        cliente.setPrimerApellido("Ficticio");
        cliente.setFechaNacimiento(LocalDate.of(1985, 5, 5));
        cliente.setTelefonoMovil("3000000001");
        cliente.setCorreo(correo);
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    private Caso nuevoCaso(Firma firma, com.example.lexcloud.entidades.Estado estado,
                           com.example.lexcloud.entidades.Subtipo_Caso subtipo,
                           Caso padre, String titulo, long radicadoBase) {
        Caso caso = new Caso();
        caso.setTitulo(titulo);
        caso.setDescripcion("Caso ficticio de prueba A4");
        caso.setFecha_Inicio(LocalDate.now());
        caso.setActivo(true);
        caso.setRadicadoInterno(String.format("%010d", radicadoBase));
        caso.setRadicadoJudicial(String.format("%023d", radicadoBase));
        caso.setEstado(estado);
        caso.setSubtipo_Caso(subtipo);
        caso.setFirma(firma);
        caso.setCasoPadre(padre);
        return casoRepository.save(caso);
    }

    private com.example.lexcloud.entidades.Vinculo_Caso nuevoVinculo(Persona persona,
            Cliente cliente, Caso caso, com.example.lexcloud.entidades.Vinculo vinculo,
            boolean autorizado) {
        com.example.lexcloud.entidades.Vinculo_Caso vc =
                new com.example.lexcloud.entidades.Vinculo_Caso();
        vc.setPersonaFirma(persona);
        vc.setCliente(cliente);
        vc.setCaso(caso);
        vc.setVinculo(vinculo);
        vc.setAutorizado(autorizado);
        return vinculoCasoRepository.save(vc);
    }

    // ---------- Cliente HTTP real con cookies ----------

    /** Login real; devuelve el header Cookie con lex_access + lex_refresh. */
    String login(String correo, String password) {
        String body = "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> resp = rest.exchange(url("/api/auth/login"),
                HttpMethod.POST, json(body), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Login falló para " + correo
                    + " con status " + resp.getStatusCode());
        }
        return cookiesDe(resp);
    }

    String cookiesDe(ResponseEntity<?> resp) {
        List<String> setCookie = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookie == null || setCookie.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String cookie : setCookie) {
            String par = cookie.split(";", 2)[0].trim();
            if (par.startsWith("lex_access=") || par.startsWith("lex_refresh=")) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(par);
            }
        }
        return sb.toString();
    }

    String url(String ruta) {
        return "http://localhost:" + puerto + ruta;
    }

    HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    HttpEntity<String> conCookies(String cookies) {
        return conCookies(cookies, null);
    }

    HttpEntity<String> conCookies(String cookies, String body) {
        HttpHeaders headers = new HttpHeaders();
        if (cookies != null && !cookies.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookies);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    ResponseEntity<String> get(String ruta, String cookies) {
        return rest.exchange(url(ruta), HttpMethod.GET, conCookies(cookies), String.class);
    }

    ResponseEntity<String> postJson(String ruta, String cookies, String body) {
        return rest.exchange(url(ruta), HttpMethod.POST, conCookies(cookies, body),
                String.class);
    }

    ResponseEntity<String> putJson(String ruta, String cookies, String body) {
        return rest.exchange(url(ruta), HttpMethod.PUT, conCookies(cookies, body),
                String.class);
    }

    ResponseEntity<String> patchSinBody(String ruta, String cookies) {
        return rest.exchange(url(ruta), HttpMethod.PATCH, conCookies(cookies),
                String.class);
    }

    ResponseEntity<String> delete(String ruta, String cookies) {
        return rest.exchange(url(ruta), HttpMethod.DELETE, conCookies(cookies),
                String.class);
    }
}
