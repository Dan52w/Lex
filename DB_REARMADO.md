# LexCloud — Rearmado de la base de datos (10-nov-2026)

Punto de partida del plan de Daniel: rearmar la BD desde cero, optimizada y
alineada al código del commit `fdfb198` (rama `rearmado-fdfb198`).
Las migraciones viven en `src/main/resources/db/migrations/`.

## Qué se entregó

| Archivo | Contenido |
|---|---|
| `V1__esquema_base.sql` | 30 tablas corregidas + 26 índices + seeds (catálogos y sistema) |
| `V2__seguridad.sql` | `refresh_tokens` y `auditoria_seguridad` con FKs e índices |
| `V3__triggers_y_reglas.sql` | 7 funciones y 8 triggers de reglas de negocio |
| `build.gradle.kts` | + `flyway-core` y `flyway-database-postgresql` |
| `application.properties` | `ddl-auto=validate` + `spring.flyway.enabled=true`, `baseline-on-migrate=true` |

Verificación hecha aquí (script `/tmp/verify_migrations.py`, **0 fallos**):
32 tablas, todas las FK resuelven en orden de creación, columnas/nulidad/
longitudes idénticas a las entidades JPA, y 12 comprobaciones de regresión
(sin `agendas.id_persona`, sin nombres viejos, maestro id 1 + `setval`,
persona sistema sembrada, trigger 008 eliminado).

## Fallos encontrados al comparar el SQL con el código

1. **El 500 de agenda (causa raíz).** El SQL creaba `agendas.id_persona`
   (FK a `personas`), pero la entidad `Agenda` y `AgendaServiceImpl` usan
   `id_abogado` (FK a `abogados`). Esa columna no existía: cualquier
   consulta de agendas fallaba. V1 crea `id_abogado` y no crea `id_persona`.
2. **Faltaban columnas que las entidades exigen.** `abogados.tarjeta_profesional`,
   `casos.radicado_interno` (VARCHAR 10, UNIQUE), `casos.radicado_judicial`
   (VARCHAR 23, UNIQUE) y `casos.id_firma`. Con `ddl-auto=validate` el
   arranque habría fallado por columnas inexistentes.
3. **Nombres de tabla distintos a las entidades.** El SQL tenía
   `tipos_empleado` y `tipos_vivienda`; las entidades piden `tipo_empleados`
   y `tipo_viviendas`. El backend buscaba tablas que no existían.
4. **Columna con nombre distinto.** `niveles_salariales.nombre` vs la
   entidad `Nivel_Salarial`, que mapea `rango`. V1 usa `rango`.
5. **El borrado lógico de casos fallaba siempre.** El trigger
   `soft_delete_casos` (013) inserta en `historial_cambios` con la persona
   sistema cc `0000000000`, que **no estaba sembrada en ningún archivo**:
   el INSERT dejaba `id_persona` en NULL y violaba el NOT NULL. V1 siembra
   la persona sistema (rol SISTEMA, sin login) y la firma sistema.
6. **El caso maestro no tenía id fijo.** `CasoServiceImpl` y
   `AgendaServiceImpl` comparan `id == 1` para identificar la raíz, pero
   el seed original dejaba el id a la secuencia (1 solo por suerte, siendo
   el primer insert). V1 lo inserta con id explícito 1 y ajusta la
   secuencia con `setval`, y le da `id_firma` (firma sistema) y radicados
   en ceros, porque la entidad exige esas columnas NOT NULL + UNIQUE.
7. **`set_root_case` creaba falsas raíces en silencio.** Si el maestro no
   existía, el `SELECT INTO` dejaba `id_caso_padre` en NULL sin error y ese
   caso quedaba protegido como si fuera el raíz. V3 ahora lanza excepción.
8. **Trigger redundante (008).** `validate_subtipo_exists` repetía lo que ya
   garantiza la FK `casos.id_subtipo_caso`; costaba un SELECT por
   INSERT/UPDATE. No se crea en V3 (y se elimina si existe en una base vieja).
9. **`equipo_abogados` no se podía reasignar.** El UNIQUE total
   `(id_abogado, id_persona)` impedía volver a asignar a alguien tras
   darlo de baja (`activo = FALSE` seguía contando). Ahora es un índice
   único parcial `WHERE activo = TRUE`. Además `activo` y `fecha_asignacion`
   eran anulables en SQL pero NOT NULL en la entidad.
10. **Orden de creación roto.** `004_Seguimiento.sql` crea `vinculo_casos`
    con FK a `clientes`, pero `clientes` se crea en el 014: en base vacía
    el 004 fallaba. V1 pone 014 y 015 antes de 004.
11. **Longitudes de catálogos menores que las entidades**
    (discapacidades 50→100, estratos 20→50, estados civiles 30→50,
    géneros 30→50, grados 60→100, tipo_relaciones 50→100, tipo_empleados
    50→100, tipo_viviendas 40→100).
12. **Detalles menores.** `DEFAULT CURRENT_TIMESTAMP` sobre columnas DATE
    pasa a `CURRENT_DATE`; `firmas.integrante` ahora `DEFAULT 0`; faltaban
    índices en casi todas las FK (agendas, documentos, historial,
    vínculos, abogados, subtipo, casos.padre/firma); `RAISE NOTICE` por
    cada borrado de persona eliminado (ruido en el log).
13. **Falta el archivo 011.** No venía entre los compartidos y nada lo
    referencia; no se inventó nada para cubrirlo.

## Decisiones que quedan anotadas

- **V2 es inerte por ahora.** `refresh_tokens` y `auditoria_seguridad` no
  las usa el código de `fdfb198`; son la base de la fase de seguridad
  (sesiones revocables + auditoría). Crearlas ahora no cambia el
  comportamiento actual.
- **V1 es solo para base nueva.** Con `baseline-on-migrate=true`, en una
  base existente V1 se marca como aplicada sin ejecutarse. El rearmado
  se prueba en una base nueva; la `db_Lex` con datos no se toca.
- **`ddl-auto=validate`.** El arranque ya no crea ni altera tablas: si el
  esquema y las entidades se desalinean, falla al arrancar en vez de
  fallar en caliente con un 500.
- **El código sigue dependiendo de `id == 1` para el maestro.** V1 lo hace
  determinista; quitar esa dependencia en el código es trabajo de la fase
  de código, no de la BD.

## Cómo correrlo en su PC (base nueva)

1. Crear una base vacía nueva (no reutilizar `db_Lex` con datos). En
   local, por ejemplo: `psql -U postgres -c "CREATE DATABASE lexcloud_rearmado;"`
   (ajustar usuario/puerto a lo que tenga instalado).
2. Apuntar `spring.datasource.url` a esa base nueva en
   `application.properties` (o con variable de entorno, sin dejar la
   URL con credenciales en el archivo).
3. `.\gradlew.bat bootRun` en `lex-backend`. Flyway ejecuta V1, V2 y V3
   en orden al arrancar.
4. Verificar:
   - `SELECT * FROM flyway_schema_history;` → 3 filas, `success = true`.
   - `SELECT id, titulo FROM casos WHERE id = 1;` → el maestro.
   - `SELECT cc FROM personas WHERE cc = '0000000000';` → la persona sistema.
   - `DELETE FROM casos WHERE id = 2;` (con un caso de prueba) → debe
     desactivarlo y dejar constancia en `historial_cambios`, no borrarlo.
5. Si el arranque falla con error de bloqueo (advisory lock) contra el
   pooler de Supabase (puerto 6543, modo transacción), correr la migración
   contra una conexión directa a Postgres (puerto 5432) o local: Flyway
   usa bloqueos de sesión y el pooler en modo transacción no los sostiene
   bien. No verificado aún; es la primera sospecha si falla.

## Pendiente de verificar (no se pudo aquí)

- **Compilación y arranque no verificados en este contenedor:** Gradle no
  levanta su daemon aquí (memoria disponible ~500 MB; el daemon muere al
  arrancar). La verificación hecha aquí es estática (0 fallos) + revisión
  de sintaxis (paréntesis y bloques `$$` balanceados, 30+2 tablas,
  7 funciones, 8 triggers). `compileJava` y `bootRun` + Flyway contra
  Postgres real quedan para su PC.
- **Ejecución real de las migraciones contra Postgres:** pendiente del
  paso 4 de arriba.

## Advertencia de seguridad (leer)

El `application.properties` de este repo (en el historial de git, desde
`fdfb198`) trae credenciales reales: la contraseña de la base de Supabase,
el secreto JWT y la llave de servicio de Supabase Storage. El repo es
privado, pero esas credenciales están en el historial y hay que **rotarlas
y sacarlas del archivo** (variables de entorno o secreto del despliegue)
antes de seguir. No se reproducen aquí por seguridad.

## Correcciones B1–B6 aplicadas en el código (10-nov-2026)

Revisión código↔BD tras las migraciones; los 6 bloqueantes quedaron
corregidos en esta misma rama (sin compilar aquí: Gradle no corre en
este contenedor; `compileJava` y `bootRun` quedan para el PC):

- **B1.** `CasoServiceImpl.crearVinculo` pedía el vínculo por id 2 con
  `getReferenceById(2)`, pero una base nueva solo tiene el vínculo id 1.
  Ahora lo busca por nombre (`findByNombre("Principal")`) y V1 siembra
  `'Principal'` además de `'SIN VÍNCULO'`. El nombre es inferido del
  código (`findAbogadoPrincipalDelCaso`), no confirmado contra la BD vieja.
- **B2.** JPQL inválida en `Abogado_Repository:23`: `a.firma = :firmaId`
  → `a.firma.id = :firmaId`.
- **B3.** Agenda con payload parcial daba 500: `guardar` pone
  `activa = true` y `descripcion = ""` si vienen en null; `actualizar`
  solo sobrescribe valores no null; `@NotNull` en `idCaso` del DTO.
- **B4.** `@NotBlank descripcion` y `@NotNull fecha_Inicio` en
  `CasoRequestDTO`.
- **B5.** `@NotNull` + `@Min(0)` en `integrante` de `FirmaRequestDTO`
  (la firma sistema tiene 0); el mensaje "NIT ya existe" solo sale
  cuando el NIT realmente existe.
- **B6.** `AuthServiceImpl.registrar` rechaza persona inactiva, persona
  sistema (cc `0000000000`) y persona que ya tiene login (400).
