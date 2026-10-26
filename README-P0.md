# LexCloud — Corrección P0 (seguridad crítica)

> **No usar con datos reales hasta cerrar P0 completo y pasar sus 12 pruebas.**
> Trabajo en local sobre el HEAD verificado del 21-jul-2026 (`fdfb198b`).
> Push a GitHub autorizado por Daniel el 25-oct-2026.
> Ver `../INVENTARIO-P0.md` para el inventario
> hallazgo por hallazgo contra el código real (ese archivo es local, no se sube).

## Qué cambió (por tarea del plan)

- **B1 — Registro cerrado.** `POST /api/auth/register` solo-admin
  (`@PreAuthorize("hasRole('ADMIN')")`). Eliminado `permitAll` de
  `/api/test/**`. No hay excepción de arranque: el primer admin se siembra
  en BD (abajo). Rotar secreto JWT y key de Supabase si hay duda de
  exposición (pendiente de Daniel: no verificable desde código).
- **B2 — Identidad desde el token.** Nuevo `security/CurrentUser`: persona,
  firma (vía registro `Abogado`), abogado y roles salen del
  `SecurityContext`. Todos los servicios ignoran `idFirma`/`idPersona` del
  cliente. Regla: **admin sin registro de abogado = acceso global** (rol de
  confianza que gestiona firmas); no-admin = solo su firma, y para casos
  además vínculo autorizado (o admin). Rutas antiguas con ids en URL se
  mantienen pero sus valores se ignoran; rutas canónicas nuevas:
  `GET /api/casos/mis-casos`, `GET /api/clientes/mi-firma`,
  `GET /api/abogados/mi-firma`, `GET /api/vinculo-casos/mis-casos`,
  `GET /api/vinculo-casos/pendientes` (filtrados por firma),
  `GET /api/agendas/mias/rango`, `GET /api/auth/me`.
- **B3 — Roles reales.** `UserDetailsImpl` normaliza a `ROLE_ADMIN`,
  `ROLE_ABOGADO`, `ROLE_CLIENTE`, `ROLE_<NOMBRE>`. `@PreAuthorize` en
  roles (clase entera), firmas (escritura), catálogos (escritura), personas,
  abogados y equipo (escritura). `Persona.actualizar()` ya NO reactiva
  (`setActivo(true)` eliminado); reactivación explícita:
  `PATCH /api/personas/{id}/reactivar` (solo admin, con auditoría). Al
  desactivar se revocan todos los refresh tokens de esa persona.
- **B4 — Vínculos.** `vincular()` ya no auto-autoriza: nace
  `autorizado=false` (salvo que lo cree un admin). El solicitante y la
  persona a vincular deben pertenecer a la firma del caso. `autorizar`
  exige admin o miembro de la firma con acceso autorizado al caso y
  distinto del vinculado (nadie se autoriza a sí mismo). `GET /pendientes`
  filtra por firma del token. Excepción documentada: al crear un caso, el
  creador recibe vínculo autorizado a **su propio caso nuevo** (no es
  auto-autorizarse a un caso ajeno).
- **B5 — Documentos.** Lista blanca `pdf/docx/xlsx/jpg/png` + magic bytes
  (docx/xlsx exigen ZIP con `[Content_Types].xml`), límite 10 MB
  (`lexcloud.documentos.max-bytes`), nombre de almacenamiento UUID del
  servidor, carpeta `firma_<id numérico>` (nunca NIT del cliente),
  descarga con `Content-Disposition: attachment` + nombre sanitizado +
  `Content-Type` de la lista blanca + `nosniff`. El borrado físico queda
  registrado en auditoría ANTES de ejecutarse (la papelera es P1-B7).
  **Decisión de Daniel (25-oct-2026):** el Supabase viejo se ignora; se
  creará un proyecto nuevo. Configurar su URL y key nuevas SOLO fuera
  del repo (en `application.properties` local o variables de entorno),
  con bucket privado. Nunca commitear la key vieja ni la nueva.
- **B6 — Sesión única revocable.** Solo cookies httpOnly `lex_access`
  (JWT corto: 15 min por defecto, pero SOLO si la propiedad no lo cambia;
  ver "Configuración verificada" abajo) y `lex_refresh` (opaco, hash
  SHA-256 en BD, rotativo, 14 días). Se eliminó el token del cuerpo
  (`AuthResponseDTO` y `LoginMapper` suprimidos por muertos) y el fallback
  Bearer del filtro; la cookie vieja `jwt_token` ya no se lee y el logout
  la borra por limpieza. `POST /api/auth/refresh` rota y emite access
  nuevo; `POST /api/auth/logout` revoca el refresh en servidor.
  Desactivar una persona revoca sus refresh (y su access expira en minutos).
  Sin `System.out`/`printStackTrace` en `JwtUtil`.
- **F1/F2/F3 (contrato para el front).** El front ya no debe leer ni
  escribir cookies de sesión (ver carpeta `lex-front`). `GET /api/auth/me`
  devuelve solo datos de presentación. El middleware del front valida la
  sesión contra `/me`.
- **A2 — CSRF.** Cookie `SameSite`/`Secure` configurables por entorno
  (despliegue cross-site: `None`+`Secure=true`; local: `Lax`+`false`) +
  `CsrfOriginFilter`: toda operación de cambio de estado con `Origin`/
  `Referer` fuera de la lista cerrada recibe 403 (login/refresh exentos:
  validan credenciales/refresh en servidor). CORS sigue siendo lista
  cerrada (verificar si el front cambió de dominio: hoy
  `localhost:3000` y `lex-cloud.vercel.app`).
- **B7 — Fuerza bruta/enumeración/contraseñas.** Login: 5 fallos por
  IP+cuenta en 15 min → 429; registro: 10 intentos/hora por IP → 429.
  Intentos fallidos auditados sin guardar la contraseña. `existe-correo`
  y `existe-cc` solo-admin. Contraseña mínima 10 (12 para admin) +
  rechazo de contraseñas comunes evidentes. Mensaje de login genérico
  conservado.
- **B8 — Secretos.** `.gitignore` cubre `.env`, `.env.*`,
  `application-*.properties` (salvo la plantilla), `*.pem`, `*.key`.
  Plantilla sin valores en
  `src/main/resources/application-example.properties`. Escaneo: en el
  código Java no hay secretos hardcodeados (solo placeholders `@Value`);
  **el historial de git no se pudo escanear desde aquí** (no hay clon
  local: la descarga fue archivo por archivo) — pendiente de Daniel:
  escanear el historial y rotar cualquier secreto real que aparezca.
  `application.properties` real fue verificado por Daniel el 25-oct-2026
  (archivo compartido fuera del repo; NO se sube, y su contenido con
  valores reales nunca se commitea). Hallazgos en "Configuración
  verificada" abajo. El historial de git sigue sin escanearse desde
  aquí; si en él aparece algún secreto real, rotarlo.
- **A3 — Auditoría mínima.** Tabla `auditoria_seguridad` (ver DDL) con
  autor = usuario del token. Eventos: `LOGIN_OK`, `LOGIN_FALLO`,
  `PERSONA_CREADA`, `USUARIO_CREADO`, `ROL_CAMBIADO`,
  `PERSONA_DESACTIVADA`, `PERSONA_REACTIVADA`, `DOCUMENTO_DESCARGADO`,
  `DOCUMENTO_ELIMINADO`, `VINCULO_CREADO`, `VINCULO_AUTORIZADO`.
  Lectura: `GET /api/auditoria/mi-firma` (solo admin, su firma). Sin
  endpoint de borrado/modificación.
- **A4 — Pruebas.** Ver `src/test/.../P0SecurityTests.java` y la tabla
  abajo. Las pruebas de integración contra API real con Postgres quedan
  como trabajo en el PC de Daniel (aquí no hay Postgres ni Docker):
  los tests incluidos son unitarios + MockMvc de seguridad.

## Configuración verificada (application.properties real, 25-oct-2026)

Sin valores reales, solo lo que cambia el arranque de P0:

- `spring.jpa.hibernate.ddl-auto=none`: Hibernate NO crea ninguna tabla.
  El DDL de abajo es **obligatorio antes de arrancar**, o el login y la
  auditoría fallarán al no existir `refresh_tokens` y
  `auditoria_seguridad`.
- `lexcloud.jwt.expiration=86400000` (24 h) en ese archivo: con ese valor
  el access dura 24 h, no 15 min. Antes de arrancar, cambiarlo a
  `900000` (15 min) para cumplir P0-B6. El secreto JWT sí cumple el
  mínimo de 32 bytes.
- Sin `spring.servlet.multipart.max-file-size`: el valor por defecto de
  Spring Boot (1 MB) rechazaría documentos de más de 1 MB antes de la
  validación P0 de 10 MB. Añadir `max-file-size=10MB` y
  `max-request-size=11MB` (ver la plantilla).
- Sin `lexcloud.cookie.samesite`/`secure`: aplican los valores por
  defecto del código (`None`+`true`, correctos para Vercel→Render). En
  local por HTTP, poner `Lax`+`false`, o la cookie no funcionará.
- Supabase viejo y cuentas viejas: ignorados por decisión de Daniel
  (nuevo proyecto Supabase; siembra desde cero, sin auditoría de
  cuentas en producción).

## DDL necesario (obligatorio con `ddl-auto=none`; también con `validate`)

```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id SERIAL PRIMARY KEY,
  id_persona INTEGER NOT NULL,
  token_hash VARCHAR(128) NOT NULL UNIQUE,
  expira_en TIMESTAMP NOT NULL,
  revocado BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS auditoria_seguridad (
  id SERIAL PRIMARY KEY,
  fecha TIMESTAMP NOT NULL,
  id_persona INTEGER,
  id_firma INTEGER,
  accion VARCHAR(60) NOT NULL,
  recurso_tipo VARCHAR(60),
  recurso_id INTEGER,
  detalle VARCHAR(500)
);
```

## Primer admin (siembra en BD, sin puerta de registro)

1. Crear el rol `Administrador` (o el nombre que use la tabla `roles`).
2. Crear la `Persona` del admin con ese rol y `activo=true`.
3. Crear su `Login` con el mismo id que la persona (`logins.id =
   personas.id`, por `@MapsId`) y la contraseña hasheada con BCrypt
   (generar el hash fuera, p. ej. con un snippet o herramienta local;
   nunca guardar la contraseña en claro ni commitear el hash).
4. A partir de ahí, ese admin crea el resto de cuentas por
   `POST /api/auth/register` y da de alta miembros por `/api/personas`
   + `/api/abogados` (flujo de 4 pasos del front, ahora solo-admin).
5. **Cuentas antiguas: ignoradas** por decisión de Daniel (25-oct-2026):
   no hay auditoría de cuentas en producción que hacer; la siembra
   parte desde cero y el Supabase viejo se sustituye por un proyecto
   nuevo.

## Verificación realmente corrida (26-oct-2026, contenedor)

- **Compilación: OK.** `mvn clean compile` en verde: 264 archivos
  fuente (ver `../maven-build/pom.xml`, pom solo para verificar aquí;
  el proyecto real sigue siendo Gradle y ese pom NO se sube al repo).
  Gradle no pudo usarse en este contenedor: su daemon es un proceso
  separado y el proxy de salida rechaza sus conexiones TCP con el
  mensaje de permisos de red; Maven, que corre en proceso, descarga
  bien por el mismo proxy. En el PC de Daniel, `gradlew.bat build`
  es la verificación canónica (pendiente de correr allí).
- **Tests: 23/23 en verde** (`mvn test`, sin BD, sin Docker):
  `FileValidationTest` 9, `P0SecurityTests` 4 (MockMvc),
  `P0UnitTests` 4, `RefreshTokenServiceTest` 3,
  `VinculoAuthorizationTest` 3. `LexCloudApplicationTests`
  (contextLoads) queda excluido: necesita BD y `application.properties`
  real y no es evidencia P0.
- **Correcciones tras la primera compilación** (errores reales
  encontrados por el compilador, no por revisión): falta el import
  de `Abogado` en `EquipoAbogadoServiceImpl` (en cascada rompía todos
  los accesores de `Equipo_Abogados`), `@Override` duplicado en
  `AgendaServiceImpl.listarMisCitasRango`, y `logger` sin declarar en
  `AuthTokenFilter` (ahora logger SLF4J propio `log`). En tests:
  campo duplicado en `VinculoAuthorizationTest`, import de
  `UserDetailsImpl` y paquete nuevo de `@WebMvcTest` en Boot 4
  (`org.springframework.boot.webmvc.test.autoconfigure`).
- **No probado contra API real aquí:** en este contenedor no hay
  Postgres/Docker. Las 12 pruebas P0-A4 de integración contra API
  real deben correrse en el PC de Daniel con BD de pruebas y datos
  ficticios. Según el propio plan, **P0 no se da por cerrado para
  uso real** hasta tener esas pruebas escritas y en verde; la tabla
  de abajo distingue cobertura unitaria/MockMvc (verde) de la
  integración pendiente.

## Limitaciones conocidas de esta entrega (honestidad)
- **Rate limiting en memoria:** por instancia. Con varias instancias
  hace falta un store compartido (P1).
- **Subida en memoria:** `MultipartFile.getBytes()` carga el archivo
  (≤10 MB tras la validación). El streaming completo a Supabase es
  mejora futura; el límite de 10 MB lo hace aceptable en P0.
- **`ddl-auto=none` (verificado):** aplicar el DDL de arriba antes de
  arrancar; con `none` Hibernate no crea `refresh_tokens` ni
  `auditoria_seguridad`.
- **Admin global:** un admin sin registro de abogado ve todas las
  firmas (decisión documentada en B2). Si se quiere admin por firma,
  hace falta modelo de admin-firma (fuera de P0).
- **Rutas antiguas:** siguen aceptando `idFirma`/`idPersona` en URL pero
  los ignoran. Eliminarlas es limpieza posterior, no seguridad, porque
  el control está en el servicio.

## Mapa pruebas P0-A4 → tests incluidos

El plan exige que las 12 sean **de integración contra la API real**;
la columna "Cobertura aquí" es solo unitaria/MockMvc (verde) y **no**
sustituye a la integración pendiente en el PC de Daniel.

| # | Prueba del plan | Cobertura aquí (verde) | Integración contra API real |
|---|---|---|---|
| 1 | A no lee/edita/borra casos de B | Ninguna (lógica: `CurrentUser.requireCaso` + `CasoServiceImpl`) | Pendiente en PC de Daniel |
| 2 | A no lee/edita/borra clientes de B | Ninguna (lógica: `ClienteServiceImpl`, firma del token) | Pendiente |
| 3 | A no descarga/borra documentos de B | Ninguna (lógica: `DocumentoServiceImpl` + `DocumentoController`) | Pendiente |
| 4 | A no ve agenda ni historial de casos de B | Ninguna (lógica: `AgendaServiceImpl`/`HistorialCambioServiceImpl`) | Pendiente |
| 5 | Sin admin no crea roles/firmas/catálogos/cambia roles/alta miembros | Parcial: MockMvc `P0SecurityTests` (register y existe-correo → 401 sin sesión, 403 sin admin) + unit roles en `P0UnitTests` | Pendiente |
| 6 | Autenticado no se registra sobre idPersona ajeno | Parcial: registro solo-admin (MockMvc 401/403); un no-admin no puede registrar nada | Pendiente |
| 7 | No cambia su propio rol ni reactiva cuenta desactivada | Ninguna dedicada (implementación: `actualizar` no toca `activo`, reactivar solo-admin) | Pendiente |
| 8 | No auto-autoriza ni ve pendientes de otra firma | Unit `VinculoAuthorizationTest` (3 tests) | Pendiente |
| 9 | Token expirado/manipulado/revocado tras logout → 401 | Unit `RefreshTokenServiceTest` (hash, rotación, revocación) | Pendiente (incluye access expirado y logout real) |
| 10 | Ejecutable/HTML/`../` rechazado o neutralizado | Unit `FileValidationTest` (9 tests: magic bytes, UUID, sanitización) | Pendiente (subida real) |
| 11 | Varios fallos de login → 429 | Unit `P0UnitTests` (bloqueo tras 5 fallos, límite de registro) | Pendiente |
| 12 | `existe-correo` sin auth no revela existencia | MockMvc `P0SecurityTests` (sin sesión → 401) | Pendiente |

## Reconciliación con el frontend P0 (26-oct-2026)

- El front ya no usa `js-cookie` para auth, usa `GET /api/auth/me`
  (campos: `personaId`, `nombreCompleto`, `correo`, `rol`, `idFirma`,
  `idAbogado`), rutas canónicas (`/mis-casos`, `/mi-firma`,
  `/mias/rango`) y middleware de servidor. `tsc` limpio y
  `next build` OK (warning: `middleware` deprecado en Next 16.2 en
  favor de `proxy.ts`; renombrar en P1).
- Rutas antiguas que el front aún usa y el backend delimita por
  token (sin IDOR): `/api/casos/persona/{id}` (ignora el id, usa el
  del token), `/api/casos/cliente/{idPersona}/{idCliente}` (ignora
  `idPersona`, usa token + cliente con vínculo autorizado),
  `/api/agendas/abogado/{id}/rango` (exige firma del abogado),
  `/api/abogados/persona/{id}` (uno mismo, admin o misma firma),
  `/api/abogados/firma/{idFirma}` (firma del token salvo admin).
- **Divergencia funcional conocida:** `equipo/[id]` pide los casos
  de otro abogado por `/api/casos/persona/{id}`, pero el backend
  devuelve los del usuario del token (seguro, pero no lo que esa
  vista espera). Queda marcado `P0-TODO-backend` en el front hasta
  que exista una ruta canónica para casos de un miembro de la firma.
- `pnpm-lock.yaml` del front aún lista `js-cookie` (se regenera con
  el próximo `pnpm install`; no se editó a mano). El front usa
  `POST /api/auth/register` y `existe-correo`/`existe-cc` solo en
  `/nuevo-miembro` (ruta solo-admin): coherente con el backend
  solo-admin. `NEXT_PUBLIC_API_URL` debe ser alcanzable desde el
  servidor Next (el middleware lo usa), no solo desde el navegador.
