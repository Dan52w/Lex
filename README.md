# LexCloud — Backend

Sistema de gestión para despachos de abogados: casos, clientes, agenda,
documentos, historial y equipo, con autorización por firma desde el
token, sesión revocable y auditoría de seguridad.

**Estado honesto (octubre 2026):** el P0 de seguridad está implementado
y publicado en `master` (backend `0c880c90`), pero **P0 NO está cerrado
para uso con datos reales**: faltan las 12 pruebas P0-A4 de integración
contra la API real con PostgreSQL (ver "Pendientes" abajo). El P1
(documentado en este README) está implementado en local en este turno y
**no se ha subido a GitHub**. `README-P0.md` queda como documento
histórico del P0; este README es el documento vivo del proyecto y
explica la relación entre ambos al final.

Sin marketing: no hay clientes, precios ni métricas que presentar.
Akmon Studios (quien mantiene este código) no está constituida como
empresa; no se presenta como tal en ningún documento del proyecto.

## 1. Arquitectura

```
Cliente (web Next.js / futura app móvil)
  │  HTTPS, cookies httpOnly (web) — ver §5
  ▼
API REST Spring Boot 4 / Java 25 (este repo)
  ├─ api/            Controladores REST (rutas /api/...)
  ├─ service/        Lógica de negocio (+ impl/)
  ├─ repositorios/   Spring Data JPA (PostgreSQL)
  ├─ entidades/      JPA (tablas, ver §4)
  ├─ dto/            request/ y response/ (validación Bean Validation)
  ├─ security/       JWT, cookies, refresh tokens, auditoría,
  │                  rate limiting, CurrentUser (identidad del token)
  ├─ exception/      GlobalExceptionHandler (errores neutros)
  ├─ util/           SearchTerm (escape de LIKE), ...
  └─ resources/db/migration/  Migraciones Flyway V1-V4 (ver §4)
```

- **Base de datos:** PostgreSQL. El esquema lo gobierna **Flyway**;
  Hibernate solo valida (`ddl-auto=validate` en despliegue).
- **Documentos:** se guardan en Supabase Storage **desde el servidor**
  (la llave nunca llega al front); la BD guarda solo metadatos y la
  ruta. Validación P0-B5: lista blanca (pdf/docx/xlsx/jpg/png),
  verificación de magic bytes, límite 10 MB, nombre de archivo UUID en
  el servidor.
- **Sin Redis, sin S3 propio, sin WebSocket** en esta fase.

## 2. Requisitos

- JDK 25 (el backend LexCloud corre con Java 25; no mezclar con el JDK
  17 de otros proyectos).
- PostgreSQL 15+ (local o Supabase).
- Gradle (wrapper incluido) para el build canónico.
- Node/pnpm solo para el front (repo separado `Dan52w/Lex-Front`).

## 3. Cómo correr

### 3.1 En Windows (PowerShell) — el entorno de desarrollo principal

```powershell
# 1. Base de datos local (o usar la de Supabase; ver §4)
#    Crear la BD vacía una sola vez:
psql -U postgres -c "CREATE DATABASE db_Lex;"

# 2. Copiar la plantilla y rellenarla FUERA del repo (nunca commitear
#    application.properties con valores reales):
Copy-Item src/main/resources/application-example.properties `
          src/main/resources/application.properties
#    Editar application.properties: contraseña de Postgres, secreto JWT
#    (>= 32 bytes), URL y llave del proyecto Supabase NUEVO, etc.

# 3. Primera vez en una base EXISTENTE con datos (db_Lex vieja):
#    Flyway con baseline-on-migrate=true marca V1 como aplicada sin
#    ejecutarla y solo corre V2-V4. Es la opción por defecto en la
#    plantilla. Alternativa manual sin Flyway (mismo efecto):
psql -U postgres -d db_Lex -f ..\..\LexCloud_P0_solo.sql

# 4. Sembrar el primer admin (ver README-P0.md, sección "Primer admin"):
#    insertar persona + rol admin + login con contraseña hasheada con
#    BCrypt. Sin este paso no hay forma de entrar: el registro público
#    está cerrado desde P0 (solo admins crean cuentas).

# 5. Arrancar:
.\gradlew.bat bootRun
#    La API queda en http://localhost:8080

# 6. Tests y build canónico:
.\gradlew.bat build
```

### 3.2 En local (contenedor de verificación, solo compilación/tests)

Gradle no funciona en el contenedor de verificación (su daemon es un
proceso aparte y el proxy de salida rechaza sus conexiones TCP). Por
eso la verificación en contenedor usa un pom Maven aparte
(`../maven-build/pom.xml`, **no forma parte del repo y no se sube**):

```bash
export JAVA_HOME=~/workspace/.tools/jdk25
~/workspace/.tools/apache-maven-3.9.11/bin/mvn -f ../maven-build/pom.xml \
  -Dtest='!LexCloudApplicationTests' test
```

`LexCloudApplicationTests.contextLoads` se excluye porque necesita una
BD real y un `application.properties` con valores reales; no es
evidencia P0/P1.

## 4. Migraciones Flyway y base de datos

| Migración | Contenido | Base nueva | Base existente |
|---|---|---|---|
| V1 `baseline` | Esquema consolidado de los 14 SQL originales (clientes antes de `vinculo_casos`; nombres `tipo_empleados`/`tipo_viviendas` ya corregidos) | Se ejecuta | Se marca aplicada con `baseline-on-migrate=true`, **sin ejecutarse** |
| V2 `p0_refresh_auditoria` | Tablas `refresh_tokens` y `auditoria_seguridad` | Se ejecuta | Se ejecuta (`IF NOT EXISTS`) |
| V3 `alinear_esquema_entidades` | Alineación con las entidades Java (ver abajo) | Se ejecuta | Se ejecuta (todo condicional) |
| V4 `papelera_e_indices` | Columnas de papelera en `documentos` + índices | Se ejecuta | Se ejecuta (`IF NOT EXISTS`) |

**Cambios de esquema de V3, uno por uno (y por qué):**

1. **Renombres** `tipos_empleado` → `tipo_empleados` y `tipos_vivienda`
   → `tipo_viviendas` (solo si la base vieja aún tiene los nombres
   viejos; PostgreSQL actualiza las FK que los referencian). Motivo: el
   SQL original usaba nombres distintos a las entidades Java.
2. **`abogados.tarjeta_profesional`** (VARCHAR 255, NOT NULL). Motivo:
   la entidad la exige NOT NULL y el SQL no la tenía. Filas existentes
   quedan con `''` y **deben corregirse a mano** con el número real de
   tarjeta de cada abogado.
3. **`casos.radicado_interno`** (VARCHAR 10), **`casos.radicado_judicial`**
   (VARCHAR 23) e **`casos.id_firma`** (FK a `firmas`, NULLABLE).
   Motivo: la entidad y `CasoServiceImpl` ya los usan; el SQL no los
   tenía. `id_firma` es NULLABLE a propósito: el caso raíz sentinel
   (`CASO MAESTRO SISTEMA`, id 1) no tiene firma.
4. **`agendas.id_abogado`** (FK a `abogados`, con backfill desde
   `abogados.id_persona` cuando hay correspondencia). Motivo: la
   entidad `Agenda` y `AgendaServiceImpl` usan abogado, no persona; el
   SQL viejo tenía `id_persona`. **La columna vieja `agendas.id_persona`
   se CONSERVA como obsoleta en P1** (no se borra hasta verificar que
   ningún dato depende de ella); el código nuevo usa `id_abogado`.
   Sin esta columna, GET/POST/PUT de agenda fallaban con 500 (ver §9).
5. **Caso raíz sentinel:** se le asignan radicados `0000000000` /
   `00000000000000000000000` solo si estaban en NULL. Es un marcador
   interno, no un radicado judicial real.

**Cambios de V4:** `documentos.eliminado_en` (NULL = activo) y
`documentos.eliminado_por` (papelera, ver §7), más índices en
`documentos(id_caso)`, `casos(id_firma)`, `auditoria_seguridad` y
`refresh_tokens`.

**Reglas:** nunca editar una migración ya aplicada; los cambios van en
migraciones nuevas (V5, V6…). Nunca `DROP DATABASE` ni borrados de
datos en migraciones.

**Script consolidado manual (sin Flyway):**
`LexCloud_DB_completa_P0.sql` (base nueva, 32 tablas) y
`LexCloud_P0_solo.sql` (base existente) se mantienen actualizados con
los mismos cambios P1 en su sección final. El archivo 011 original no
existe y nada lo referencia; no se inventó contenido para él.

**Aviso sin resolver (decisión de Daniel, pendiente):** el trigger
`soft_delete_casos` inserta en `historial_cambios` con la persona
sistema cc `0000000000`, que **no está sembrada** en ningún SQL. Borrar
un caso antes de crear esa persona hará fallar el trigger. No se sembró
una persona inventada: falta decidir su rol y sus datos. Opción
documentada en `docs-p1/P1-A2-operacion-backups-cifrado-incidentes.md`.

## 5. Autenticación por cookies y roles

- **Sesión web:** solo cookies `httpOnly` fijadas por el backend:
  `lex_access` (JWT, 15 min) y `lex_refresh` (token opaco, 14 días,
  rotativo y revocable, guardado hasheado SHA-256 en `refresh_tokens`).
  **Ningún token viaja en el cuerpo de la respuesta ni es legible desde
  JavaScript.**
- **Login:** `POST /api/auth/login` (público, con rate limiting: 5
  fallos/15 min → 429). **Registro:** `POST /api/auth/register` solo
  admin. **Logout:** `POST /api/auth/logout` revoca el refresh en
  servidor. **Refresh:** `POST /api/auth/refresh` rota el refresh.
  **Perfil:** `GET /api/auth/me` (datos de presentación).
- **Cookies por entorno:** local `SameSite=Lax` + `Secure=false`
  (HTTP); despliegue cross-site (front Vercel → back Render)
  `SameSite=None` + `Secure=true` (HTTPS). Ver
  `application-example.properties`.
- **Roles:** `ROLE_ADMIN`, `ROLE_ABOGADO`, `ROLE_ASISTENTE` (normalizados
  con prefijo `ROLE_` en P0). `@PreAuthorize` en los controladores;
  la identidad y la firma se derivan **siempre del token**
  (`CurrentUser`), nunca de ids que envíe el cliente. Un admin sin
  registro de abogado tiene acceso global (decisión P0 documentada).
- **CORS:** lista cerrada (`lexcloud.cors.allowed-origins`, hoy
  `http://localhost:3000` y `https://lex-cloud.vercel.app`); sin `*`
  con credenciales. El filtro de origen (`CsrfOriginFilter`) protege
  las operaciones de cambio de estado (login/refresh exentos).
- **Alta de miembro transaccional (P1-F4):** `POST /api/miembros`
  (solo admin) crea persona + abogado + login + asignación de equipo
  opcional en **una sola transacción**: si algo falla, no queda nada.
  Reemplaza la cadena de 4 POST del front antiguo.

## 6. Endpoints principales

Base: `/api`. Todos exigen sesión salvo login/refresh/logout.

| Recurso | Ejemplos |
|---|---|
| Auth | `POST /auth/login`, `POST /auth/logout`, `POST /auth/refresh`, `GET /auth/me`, `POST /auth/register` (admin) |
| Casos | `GET /casos/mis-casos`, `GET /casos/{id}`, `GET /casos/miembro/{idAbogado}` (canónica de equipo, P1), `POST /casos`, `PUT /casos/{id}`, búsquedas por radicado/título/estado/cliente |
| Clientes | `GET /clientes/mi-firma`, `GET /clientes/buscar?filtro=…`, `POST /clientes`, `PUT /clientes/{id}` |
| Agenda | `GET /agendas/mias/rango`, `GET /agendas/abogado/{id}/rango`, `POST /agendas`, `PUT /agendas/{id}`, `DELETE /agendas/{id}` |
| Documentos | `POST /documentos` (multipart), `GET /documentos/caso/{id}`, `GET /documentos/descargar/{id}`, `DELETE /documentos/{id}` (a papelera), `GET /documentos/papelera`, `PUT /documentos/{id}/restaurar`, `DELETE /documentos/{id}/definitivo` (admin) |
| Equipo | `GET /equipo/activos`, `POST /equipo`, `GET /equipo/abogado/{id}` |
| Miembros | `POST /miembros` (alta transaccional, admin) |
| Auditoría | `GET /auditoria/mi-firma` (admin) |
| Catálogos | `GET /tipos-…`, `GET /firmas`, etc. (listados pequeños de referencia) |

**Paginación (P1-B2):** los listados principales devuelven
`PageResponse { content, page, size, totalElements, totalPages, last }`
y aceptan `?page=` y `?size=` (tamaño máximo 50, por defecto 20; un
tamaño mayor se recorta, nunca se devuelve la lista completa). Aplica a
mis-casos, clientes, documentos por caso, agenda (por caso, por rango
propio/de abogado/de caso), historial, auditoría y abogados de la
firma. La ruta canónica `GET /casos/miembro/{idAbogado}` devuelve
**array sin paginar a propósito**: la vista `equipo/[id]` hace
`casos.length` y `map` sobre el array (si se pagina después, esa vista
debe cambiar a la vez). **Excepción documentada:** los catálogos pequeños
de referencia (tipos, géneros, estados civiles…) siguen devolviendo
listas completas porque son datos sembrados de pocas decenas de filas;
si crecen, se paginan igual. Las búsquedas `LIKE` escapan `%`, `_` y
`\` (`SearchTerm.escape` + `ESCAPE '\'` en JPQL) para que un filtro no
se convierta en comodín.

## 7. Papelera y retención de documentos (P1-B7)

- `DELETE /documentos/{id}` **ya no borra físicamente**: envía a
  papelera (`eliminado_en`, `eliminado_por`), conserva fila y archivo,
  y queda auditado (`DOCUMENTO_PAPELERA`). Los documentos en papelera
  no aparecen en el listado del caso ni se pueden descargar.
- `PUT /documentos/{id}/restaurar` devuelve el documento a activo
  (auditado como `DOCUMENTO_RESTAURADO`).
- `DELETE /documentos/{id}/definitivo` (solo admin) borra archivo y
  fila, auditado **antes** de ejecutarse
  (`DOCUMENTO_ELIMINADO_DEFINITIVO`).
- **Retención documentada:** 30 días en papelera; pasado ese plazo, el
  admin puede borrar definitivamente. **No hay purga automática en P1**
  (no se afirma que exista un job que no existe).

## 8. Auditoría (P1-A1)

`auditoria_seguridad` registra autor (del token), firma, acción,
recurso, fecha y detalle (truncado a 500 caracteres). Acciones P0:
login ok/fallo, usuario creado, rol cambiado, persona
desactivada/reactivada, documento descargado/eliminado, vínculo
creado/autorizado. Acciones P1 añadidas en código: caso creado/editado, cliente
creado/editado/desactivado, documento a papelera/restaurado/eliminado
definitivamente, miembro creado. Consulta: `GET /auditoria/mi-firma`
(solo admin de su firma; admin global sin firma ve todos los
registros). Sin endpoint de borrado: la auditoría es inmutable desde la
UI. El documento completo de eventos pendientes y campos está en
`docs-p1/P1-A1-auditoria-completa.md`.

## 9. Errores conocidos y su corrección (Agenda 500)

**Síntoma (reportado por Daniel):** la agenda devolvía 500 en GET y
POST (y PUT compartía la causa).

**Causa raíz:** la base de datos del usuario tenía el esquema viejo
(`agendas.id_persona`) con `ddl-auto=none`, mientras la entidad
`Agenda` y `AgendaServiceImpl` ya usaban `id_abogado`. Cualquier
consulta de agenda fallaba porque la columna `id_abogado` no existía.
El error no era de la contraseña ni del front: era esquema contra
entidad.

**Corrección (P1, coherente, sin dos columnas competidoras sin
documentar):** la migración V3 añade `id_abogado` con backfill desde
`abogados.id_persona`, y `agendas.id_persona` queda documentada como
obsoleta (se conserva en P1, no se borra). El código ya usaba
`id_abogado`; no se cambió el modelo a `id_persona`. Endurecimiento
opcional posterior (solo si `SELECT COUNT(*) FROM agendas WHERE
id_abogado IS NULL` da 0): `ALTER TABLE agendas ALTER COLUMN
id_abogado SET NOT NULL`, para igualar el `nullable = false` de la
entidad; si quedan filas en NULL, esas personas no tienen registro en
`abogados` y necesitan decisión manual antes de endurecer.
Tres roturas más, tapadas por el 500 de la columna, se corrigieron en
el mismo flujo: POST daba siempre 400 porque el front ya no envía
`idAbogado` y el DTO lo exigía (ahora opcional y derivado del token);
PUT daba siempre 404 porque la respuesta no incluía `idAbogado` y el
front reenviaba 0 (ahora la respuesta lo incluye y null/0 conserva el
valor del recurso); y nulls en `descripcion`/`activa` daban 409 en vez
de 400 (ahora `@NotBlank`/`@NotNull` y defaults). Tests MockMvc de
agenda (12: éxito, 401 sin sesión, 403 de otra firma, validación,
POST sin `idAbogado`, PUT con `idAbogado=0`, ruta miembro) en
`AgendaP1Tests` — ver §10.

**Otros errores del mismo origen, ya cubiertos por V3:** `casos` sin
radicados/`id_firma` y `abogados` sin `tarjeta_profesional` habrían
fallado igual al crear casos/abogados en una base vieja.

## 10. Tests

- **Unitarios/MockMvc (P0, 23 tests):** `FileValidationTest` (9),
  `P0SecurityTests` (4), `P0UnitTests` (4), `RefreshTokenServiceTest`
  (3), `VinculoAuthorizationTest` (3).
- **P1:** `AgendaP1Tests` (GET/POST/PUT: éxito, 401, 403, validación).
- **CI (P1-B6):** `.github/workflows/ci.yml` (backend: build + tests +
  revisión de dependencias en PR + escaneo de secretos con gitleaks) y
  su equivalente en el repo del front. **Estos archivos quedan en local
  en el turno P1 y NO se subieron a GitHub**: Daniel los sube cuando lo
  ordene (por la web o con la tarjeta de aprobación desde el chat
  principal; subir workflows por API puede devolver 403 si la app no
  tiene permiso "Workflows").
- **Bloqueo de versiones (P1-B6):** `dependencyLocking` activado en
  `build.gradle.kts`; el `gradle.lockfile` se genera en el PC de Daniel
  con `./gradlew dependencies --write-locks` (Gradle no corre en el
  contenedor de verificación, por eso no hay lockfile en este turno).
  Dependabot semanal en `.github/dependabot.yml`.

## 11. Seguridad y cabeceras (P1-B4)

Backend (`WebSecurityConfig`): CSP restrictiva (`default-src 'none';
frame-ancestors 'none'` — la API no sirve HTML), HSTS (solo efectivo
sobre HTTPS; en local HTTP los navegadores lo ignoran),
`Referrer-Policy: STRICT_ORIGIN_WHEN_CROSS_ORIGIN`,
`Permissions-Policy: camera=(), microphone=(), geolocation=(),
payment=()`; `nosniff` y `X-Frame-Options: DENY` ya venían por defecto
de Spring Security. Front: CSP coordinada en `next.config.ts` (sin
HSTS en desarrollo local; HSTS solo en despliegue con HTTPS).

Errores neutros: `GlobalExceptionHandler` ya no devuelve mensajes de
excepción crudos (que podían filtrar ids/radicados); el detalle queda
solo en el log. El front no muestra mensajes crudos del backend ni usa
`alert()`.

## 12. Despliegue

- **Backend:** Docker (ver `Dockerfile`: build con tests, runtime JRE
  25, usuario no root `appuser`, sin secretos en la imagen, healthcheck
  de puerto). Variables de entorno en el proveedor (Render u otro),
  nunca en la imagen ni en el repo.
- **Front:** Vercel (repo `Dan52w/Lex-Front`); `NEXT_PUBLIC_API_URL`
  debe ser alcanzable desde el servidor Next (el middleware lo usa),
  no solo desde el navegador.
- **Supabase:** proyecto **nuevo** (el viejo se ignora por decisión de
  Daniel); bucket privado; la llave vive solo en el servidor. Verificar
  y anotar con fecha: cifrado en reposo, SSL y backups del proyecto
  nuevo (`docs-p1/P1-A2-operacion-backups-cifrado-incidentes.md`).
- **Antes de desplegar con datos reales:** `ddl-auto=validate`,
  cookies `None`+`true`, Flyway aplicado, primer admin sembrado, y las
  12 pruebas P0-A4 en verde (ver §13).

## 13. Pendientes honestos (lo que NO está cerrado)

1. **P0-A4 (bloqueante para uso real):** 12 pruebas de integración
   contra la API real con PostgreSQL en el PC de Daniel. Sin ellas,
   LexCloud no debe usarse con datos reales de clientes.
2. **Build canónico en su PC:** `.\gradlew.bat build` (en el
   contenedor solo se verificó con Maven; el build Gradle real, el
   `gradle.lockfile` y Flyway contra una BD viva están pendientes de
   su PC).
3. **Front OOM reportado por Daniel (25-oct):** al correr el front le
   salió `JavaScript heap out of memory` con heap de ~11 MB y varios
   workers cayendo a la vez; no apunta al código P0 (compila limpio en
   contenedor). Fix prescrito: borrar `.next` y correr con
   `$env:NODE_OPTIONS="--max-old-space-size=4096"` (o `2048` si su PC
   tiene 8 GB o menos). Diagnósticos pedidos y aún no recibidos:
   comando exacto (`pnpm dev` vs `pnpm build`), `node -v`, `pnpm -v`,
   `$env:NODE_OPTIONS`, memoria libre y primeras 20–30 líneas del log.
4. **Configuración en su PC:** aplicar el `application.properties`
   local corregido (expiración 900000, multipart 10/11 MB, cookies
   `Lax`+`false`, `ddl-auto=update` solo en local) y reintentar login;
   en despliegue volver a `validate` y cookies `None`+`true`.
5. **Persona sistema cc `0000000000`:** decidir rol y datos (o aceptar
   que el trigger `soft_delete_casos` falle al borrar un caso).
6. **Dominio del front para CORS:** confirmar que no cambió
   (`lex-cloud.vercel.app`); si cambió, actualizar la lista cerrada.
7. **Supabase nuevo:** crear el proyecto, bucket privado, y poner sus
   valores solo fuera del repo.
8. **Documentos P1-A3 (legales):** el aviso de privacidad y el contrato
   de encargo en `docs-p1/` son borradores con marcadores
   (`[NOMBRE/IDENTIFICACIÓN A COMPLETAR POR EL DESPACHO]`); **un
   abogado debe validarlos antes de cualquier uso real**. RNBD ante la
   SIC: decisión pendiente de abogado, no asumida.
9. **Diferidos de P1-B8 (no hechos en este turno, con motivo):**
   renombre del paquete `com.example.lexcloud` y corrección de typos
   (`AgentaService`, `TipoAgentaResquestDTO`,
   `TipoIdenficacionResponseDTO`) — se hacen en una rama dedicada
   porque tocan decenas de archivos y no aportan seguridad; generar el
   radicado judicial desde una secuencia dedicada en vez de
   zero-padding (cambia la semántica del radicado y necesita decisión
   de Daniel); eliminar la dependencia del caso raíz id 1 (cambio de
   modelo con riesgo, necesita su decisión).
10. **Equipo/[id] (resuelto en P1):** la ruta canónica
    `GET /casos/miembro/{idAbogado}` existe en el backend y el front
    ya la usa (`equipo/[id]` dejó de llamar a
    `/api/casos/persona/{idPersona}`, que devolvía los casos del
    usuario del token).

## 14. Relación entre README.md y README-P0.md

- `README-P0.md`: documento histórico del P0 (qué se cambió, DDL de
  `refresh_tokens`/`auditoria_seguridad`, siembra del primer admin,
  sección "Configuración verificada" con los hallazgos del
  `application.properties` real). Se conserva sin reescribir: es la
  evidencia de lo que se hizo en P0.
- `README.md` (este archivo): documento vivo. Resume P0 y documenta
  P1, migraciones, papelera, paginación, alta transaccional y
  pendientes. Cuando P1 se suba y se cierre, este README se actualiza;
  `README-P0.md` no se toca.

## 15. Variables de entorno (sin valores reales)

Ver `src/main/resources/application-example.properties` (plantilla;
copiar a `application.properties` fuera del repo o usar variables de
entorno). Lista: `spring.datasource.url/username/password`,
`lexcloud.jwt.secret` (>= 32 bytes), `lexcloud.jwt.expiration`
(900000), `lexcloud.jwt.refresh-expiration-days` (14),
`lexcloud.cookie.samesite/secure`, `lexcloud.cors.allowed-origins`,
`lexcloud.documentos.max-bytes` (10485760),
`spring.servlet.multipart.max-file-size/max-request-size` (10MB/11MB),
`supabase.storage.url/key` (solo servidor),
`spring.flyway.enabled/baseline-on-migrate` (true en base existente),
`spring.jpa.hibernate.ddl-auto` (`validate` en despliegue).
