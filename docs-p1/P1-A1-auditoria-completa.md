# P1-A1 — Auditoría completa y trazabilidad (LexCloud)

**Fase:** P1-A1 del Plan LexCloud P0/P1/P2. **Estado:** documento de especificación.
**No es código.** Indica qué falta implementar; ningún archivo de este documento
modifica el backend ni el frontend.
**Nota:** este plan no es asesoría legal. Akmon Studios no está legalizada:
no se la presenta aquí como empresa constituida, y no se inventan clientes,
precios, métricas ni testimonios.

## 1. Qué ya existe en P0 (verificado en el código actual)

Base: entidad `AuditoriaSeguridad` (tabla `auditoria_seguridad`), servicio
`AuditoriaService` (transacción `REQUIRES_NEW`, autor siempre desde el token
vía `CurrentUser`, nunca desde el DTO; detalle truncado a 500 caracteres;
nunca guarda contraseñas, tokens ni contenido de documentos) y controlador
`AuditoriaController` con `GET /api/auditoria/mi-firma` solo-admin
(`@PreAuthorize("hasRole('ADMIN')")`). No existe endpoint de borrado ni de
modificación de registros de auditoría.

Eventos P0 ya registrados (acciones canónicas declaradas en `AuditoriaService`
y verificadas en `AuthController`, `DocumentoController`,
`DocumentoServiceImpl`, `PersonaServiceImpl` y `VinculoCasoServiceImpl`):

| Evento | Acción |
|---|---|
| Inicio de sesión correcto / fallido | `LOGIN_OK`, `LOGIN_FALLO` (sin guardar la contraseña intentada) |
| Creación de usuario/persona | `USUARIO_CREADO` / `PERSONA_CREADA` |
| Cambio de rol | `ROL_CAMBIADO` (solo cuando el rol realmente cambia) |
| Desactivación / reactivación de persona | `PERSONA_DESACTIVADA`, `PERSONA_REACTIVADA` |
| Descarga de documento | `DOCUMENTO_DESCARGADO` |
| Borrado de documento | `DOCUMENTO_ELIMINADO` (registrado antes de borrar) |
| Creación / autorización de vínculo de caso | `VINCULO_CREADO`, `VINCULO_AUTORIZADO` |

Campos actuales por registro: `fecha`, `personaId` (autor, del token),
`firmaId`, `accion`, `recursoTipo`, `recursoId`, `detalle` (referencia corta).

## 2. Qué debe auditarse además del mínimo P0

### 2.1 Operaciones sobre casos y clientes (hoy SIN auditoría)

Verificado: `CasoServiceImpl` y `ClienteServiceImpl` no usan
`AuditoriaService`. Toda esta tabla es trabajo pendiente de implementación:

| Evento | Acción propuesta | Recurso |
|---|---|---|
| Crear caso | `CASO_CREADO` | caso, id del caso |
| Editar caso (título, descripción, radicados, subtipo) | `CASO_EDITADO` | caso, id del caso |
| Desactivar / borrar lógico de caso | `CASO_DESACTIVADO` | caso, id del caso |
| Cambio de estado de caso | `CASO_ESTADO_CAMBIADO` | caso, id + estado anterior y nuevo (solo nombres de estado, no contenido) |
| Crear cliente | `CLIENTE_CREADO` | cliente, id del cliente |
| Editar cliente | `CLIENTE_EDITADO` | cliente, id del cliente |
| Desactivar cliente | `CLIENTE_DESACTIVADO` | cliente, id del cliente |
| Subir documento (hoy solo se auditan descarga y borrado) | `DOCUMENTO_SUBIDO` | documento, id del documento |
| Restaurar documento desde papelera (P1-B7) | `DOCUMENTO_RESTAURADO` | documento, id del documento |
| Borrado definitivo desde papelera (P1-B7) | `DOCUMENTO_BORRADO_DEFINITIVO` | documento, id del documento |
| Crear / editar / borrar agenda de un caso | `AGENDA_CREADA`, `AGENDA_EDITADA`, `AGENDA_ELIMINADA` | agenda, id de la agenda + id del caso |
| Crear / editar equipo de abogados de un caso | `EQUIPO_CREADO`, `EQUIPO_EDITADO` | vínculo/equipo, id + id del caso |
| Eliminar vínculo de caso (hoy solo creación y autorización) | `VINCULO_ELIMINADO` | vinculo_caso, id del vínculo |

### 2.2 Sesión y seguridad

| Evento | Acción propuesta | Nota |
|---|---|---|
| Cierre de sesión | `LOGOUT` | Hoy el logout revoca el refresh pero no deja registro. Registrarlo. |
| Renovación de refresh / revocación por reutilización | `REFRESH_ROTADO`, `REFRESH_REVOCADO` | La reutilización de un refresh ya rotado es señal de robo de sesión: registrarla y revocar la familia completa. |
| Intento de acceso denegado (403) a un recurso de otra firma | `ACCESO_DENEGADO` | Registrar id del recurso intentado y firma del solicitante. Es el registro que delata un intento de IDOR. |
| Consulta de la propia auditoría | `AUDITORIA_CONSULTADA` | Quién consultó, cuándo y con qué filtros. Evita consultas sigilosas. |
| Cambio de contraseña / restablecimiento (cuando exista) | `CONTRASENA_CAMBIADA` | Nunca guardar la contraseña nueva ni la anterior. |

### 2.3 Lecturas y accesos: qué se registra y qué NO

El plan P1-A1 pide auditar "accesos a casos sensibles si se definen como
tales". Decisión de este documento, para no convertir la auditoría en un
registro masivo que nadie revisa:

- **Sí se auditan siempre:** descargas de documentos (ya en P0), accesos
  denegados (403), consultas de auditoría y cualquier lectura de un caso
  marcado como sensible (cuando el despacho defina esa marca; hoy no existe
  tal marca en la entidad `Caso`).
- **No se audita cada lectura ordinaria de caso/cliente** (cada `GET`
  de listado): genera volumen sin valor, degrada rendimiento y crea un
  segundo inventario de datos personales. Si un despacho exige trazabilidad
  total de lecturas, se activa como opción por firma y con retención corta,
  nunca por defecto.
- **Exportaciones o descargas masivas:** hoy no existe endpoint de
  exportación masiva en el backend (verificado: ningún controlador expone
  exportación a CSV/XLSX/PDF masivo). Si se crea uno en el futuro, su uso
  debe auditarse obligatoriamente como `EXPORTACION_MASIVA` con el filtro
  aplicado y la cantidad de registros, y debe ser solo-admin o solo-rol
  autorizado. No se audita "descarga masiva" que no existe.

### 2.4 Campos de cada registro (ampliación respecto a P0)

Mantener los campos P0 y añadir, en la implementación futura:

- **Autor:** `personaId` y `firmaId` siempre desde el token (`CurrentUser`),
  nunca desde el DTO, body o query. Sin excepciones.
- **Recurso:** `recursoTipo` (vocabulario cerrado: `caso`, `cliente`,
  `documento`, `persona`, `vinculo_caso`, `agenda`, `auditoria`, `login`)
  y `recursoId`. Para cambios de estado, el `detalle` lleva solo
  "estado anterior → estado nuevo" (nombres de catálogo).
- **Fecha y hora:** con zona horaria de Colombia (America/Bogota) en la
  presentación; en base de datos, marca temporal sin ambigüedad.
- **Resultado:** `exito` / `fallo` (nuevo campo). Un intento denegado
  también es un registro, y debe distinguirse de una acción exitosa.
- **Origen:** dirección IP y agente de usuario (nuevo campo, opcional).
  La IP es dato personal: se conserva solo el tiempo de retención definido
  (ver §4) y no se muestra en listados ordinarios.
- **Valores antes/después:** solo para cambios de estado y de rol, y solo
  identificadores o nombres de catálogo. Nunca valores de campos personales
  (cédulas, direcciones, correos, contenido de casos o de documentos).
- **Nunca en auditoría ni en logs:** contraseñas (ni intentadas), tokens
  (access, refresh, JWT), contenido de documentos, cédulas, correos ni
  direcciones. El `detalle` es una referencia corta, no una copia de datos.

### 2.5 Campos `createdBy` / `updatedBy` en las entidades (P1-A1)

Además de la tabla de auditoría, las entidades principales (`Caso`,
`Cliente`, `Documento`, `Persona`, `Agenda`, `Historial_Cambio`) deben
llevar `createdBy`, `createdAt`, `updatedBy`, `updatedAt` resueltos desde
el token con anotaciones de Spring Data JPA (auditoría de entidades), en
vez de atribuir cambios desde DTOs. Esto es distinto de la tabla de
auditoría: es el "quién tocó por última vez este registro" visible en la
ficha, mientras la tabla de auditoría es el historial completo e
inmutable. Falta implementar en ambos lados: hoy ningún servicio asigna
estos campos desde el token.

## 3. Quién puede consultar la auditoría

- **Solo administradores de su propia firma.** La consulta filtra siempre
  por la firma del token (`currentUser.firmaId()`), sin excepción.
- **Hueco P0 a corregir en P1:** hoy, un admin sin firma asociada
  (`firmaId == null`) recibe `findAll()`, es decir, registros de TODAS las
  firmas. Ese camino solo es admisible para un rol de operación interna
  explícito y separado del admin de despacho; mientras no exista ese rol,
  un admin sin firma no debe ver registros de firmas ajenas: debe recibir
  lista vacía o error, no el total. Decisión pendiente de Daniel: crear el
  rol de operador interno o cerrar el camino `findAll()`.
- **Nunca:** abogados, asistentes ni miembros sin rol admin; nunca el
  titular de los datos sobre su propio caso (la auditoría es herramienta
  de control del despacho, no historial para el cliente); nunca usuarios
  de otra firma, aunque conozcan un id de registro.
- **La auditoría no se puede modificar desde la UI normal:** no existe ni
  debe existir endpoint de edición o borrado de registros de auditoría.
  La UI futura solo ofrece lectura con filtros (por fecha, acción, autor y
  recurso) y paginación. El borrado solo procede por retención (ver §4)
  ejecutado por operación interna documentada, nunca por un usuario del
  despacho desde la aplicación.

## 4. Qué falta implementar en código (lista de trabajo, sin código aquí)

1. Registrar los eventos de la tabla §2.1 en `CasoServiceImpl`,
   `ClienteServiceImpl`, `DocumentoServiceImpl` (subida), `AgendaServiceImpl`
   y `EquipoAbogadoServiceImpl`, siempre con autor del token.
2. Registrar `LOGOUT`, `REFRESH_ROTADO`/`REFRESH_REVOCADO` y
   `ACCESO_DENEGADO` (este último en el punto donde hoy se lanza la
   excepción de acceso, antes de responder 403).
3. Añadir campos `resultado`, `ip` y `userAgent` a `AuditoriaSeguridad`
   (migración versionada, P1-B1, nunca editando el DDL de P0 ya aplicado).
4. Cerrar el camino `findAll()` para admin sin firma (§3) o crear el rol
   de operador interno que lo justifique.
5. Paginación y filtros en `GET /api/auditoria/mi-firma`: hoy devuelve la
   lista completa sin límite (mismo problema que P1-B2). Máximo 50 por
   página, filtros por fecha, acción, autor y recurso, orden por fecha
   descendente. Devolver DTO, no la entidad JPA directa.
6. Unificar el vocabulario de acciones: hoy conviven `USUARIO_CREADO` y
   `PERSONA_CREADA` para el mismo hecho. Definir un catálogo único y
   documentarlo; no cambiar el significado de acciones ya registradas.
7. Protección a nivel de base de datos: revocar `UPDATE` y `DELETE` sobre
   `auditoria_seguridad` al usuario de la aplicación (solo `INSERT` y
   `SELECT`), y/o trigger que impida modificar filas. Sin esto, un fallo
   en la API podría permitir alterar el registro.
8. Política de retención documentada y aplicada: conservar la auditoría
   un mínimo útil para investigación (sugerencia: 2 años) y borrar por
   lotes solo por operación interna con acta, nunca desde la UI.
9. Pantalla de consulta en el front solo para admin, con los mismos
   filtros y mensajes neutros ante error (P1-F3). Hoy no existe ninguna
   pantalla de auditoría en el front.
10. Revisión periódica: el admin de cada firma revisa su auditoría al menos
    mensualmente (eventos `ACCESO_DENEGADO`, `LOGIN_FALLO` repetidos y
    `DOCUMENTO_ELIMINADO`). Sin revisión, el registro no sirve.

## 5. Criterio de aceptación de P1-A1

- Crear, editar, desactivar un caso y un cliente, y cambiar el estado de
  un caso, dejan cada uno su registro con autor del token, firma, recurso
  y fecha, consultable solo por el admin de esa firma.
- Un admin de la firma A no ve registros de la firma B bajo ningún camino
  (incluido el caso "admin sin firma").
- No existe endpoint ni botón que edite o borre un registro de auditoría.
- La consulta pagina (máximo 50) y filtra por fecha, acción, autor y recurso.
- Un intento de acceso a un caso ajeno deja un registro `ACCESO_DENEGADO`
  y devuelve 403, sin entregar datos.
