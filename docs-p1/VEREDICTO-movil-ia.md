# VEREDICTO — App móvil e IA sobre el backend actual de LexCloud

**Tipo:** documento de veredicto. **No se construye ninguna app ni
ninguna IA aquí.** Solo se evalúa, con evidencia del código actual, si
es viable y en qué orden.
**Base verificada (octubre 2026, rama master P0):** backend
`Dan52w/Lex` (commit `0c880c90`), front `Dan52w/Lex-Front` (commit
`b5806072`). Sesión actual: un solo modelo en cookies httpOnly
`lex_access` (JWT corto) + `lex_refresh` (token opaco rotativo y
revocable), fijadas por el backend (`SessionCookies`), sin Bearer y sin
token en el cuerpo. CORS: lista cerrada (`lexcloud.cors.allowed-origins`,
por defecto `http://localhost:3000,https://lex-cloud.vercel.app`) con
credenciales. Autorización: identidad y firma desde el token
(`CurrentUser`), `requireCaso()` por vínculo autorizado, roles con
`@PreAuthorize`. Auditoría: `AuditoriaSeguridad` + `GET
/api/auditoria/mi-firma` solo-admin. **No existe código móvil ni de IA
en ninguno de los dos repos** (sin React Native/Expo/Capacitor, sin
Groq/OpenAI/LLM, sin endpoint de chat ni de resumen): todo lo móvil y
lo de IA es trabajo nuevo, no una extensión de algo existente.
**Nota:** este veredicto no es asesoría legal. Akmon Studios no está
legalizada: no se la presenta como empresa constituida.

## 1. La pregunta, desambiguada (porque "automáticamente" es ambigua)

El usuario formula la IA como "consultar los casos de un abogado
automáticamente". Se evalúan todas las lecturas, porque cada una cambia
el veredicto:

- **Lectura (i) — por número de abogado:** "número" puede significar
  (a) identificador interno del abogado, (b) tarjeta profesional, o
  (c) número de teléfono. Solo (a) y (b) son identificadores usables, y
  solo bajo la regla del §3.1. **(c) nunca:** un teléfono no es un
  identificador de autorización; buscar casos por teléfono reintroduce
  la enumeración que P0-B7 cerró y expone casos a quien conozca un
  número.
- **Lectura (ii) — por radicado / número de caso:** búsqueda por
  radicado judicial o radicado interno. Es la lectura más segura (ver §3.2).
- **Lectura de "automático" como consulta programada/periódica:** un
  proceso que consulta casos cada cierto tiempo y los pasa por una IA
  sin que nadie lo pida. **No recomendada** (ver §4.3).
- **Lectura de "automático" como bajo demanda:** el abogado abre la app
  o escribe una pregunta y el sistema consulta sus casos en ese momento.
  **Recomendada** (ver §4.2).

## 2. VEREDICTO GENERAL

**Veredicto: VIABLE CON CAMBIOS PREVIOS. No hace falta reestructurar
desde cero.**

El backend actual se rescata casi entero: su modelo de autorización por
firma desde el token, su sesión revocable, su auditoría y su
almacenamiento de documentos son exactamente la base que una app móvil
y una IA necesitan. Lo que hay que construir es nuevo, pero encima de
esa base, no en su lugar. Reestructurar desde cero solo se justificaría
si se quisiera cambiar de stack, de modelo de datos o de proveedor, y
ninguna de esas razones existe hoy.

La excepción es una sola: **la IA "automática" tal como suena — que
consulte casos periódicamente y los envíe completos a un LLM externo —
NO es viable ni recomendable.** Esa versión se rechaza y se sustituye
por la versión bajo demanda y minimizada del §4.2. Si alguien exige la
versión automática con casos completos a un LLM externo, entonces sí
habría que decir que no, no que reestructurar.

## 3. App móvil con el mismo backend: viable, con estos cambios previos (en orden)

### Condición 0 — Antes de todo: cerrar P0 para uso real

Las 12 pruebas P0-A4 en verde contra API real con Postgres en el PC de
Daniel. Sin esto, ni web ni móvil deben tocar datos reales. Esto no es
un requisito móvil: es el requisito general pendiente.

### Condición 1 — Nuevo modelo de sesión para móvil (el cambio imprescindible)

**Por qué:** las apps móviles nativas no comparten cookies con el
navegador igual que la web. La cookie httpOnly `lex_access`/`lex_refresh`
funciona en el navegador (el front Next.js la recibe con
`credentials: 'include'` y nunca la lee desde JavaScript), pero una app
nativa no tiene ese mismo almacén de cookies ligado al dominio del
front, y depender de cookies del navegador en móvil es frágil y rompe
el modelo "un solo modelo de sesión" de P0-B6/F1.

**Qué hay que construir (backend, trabajo nuevo):**
1. Un endpoint de login específico para móvil que, tras validar
   credenciales y aplicar el mismo rate limiting de P0-B7, entregue un
   access token corto y un refresh token rotativo y revocable — el mismo
   inventario de `refresh_tokens` que ya existe, reutilizado, no uno
   paralelo.
2. La app guarda el access solo en memoria y el refresh solo en
   almacenamiento seguro del sistema (Keychain en iOS, Keystore en
   Android, vía la librería de almacenamiento seguro del framework
   elegido). **Nunca** en almacenamiento inseguro (ver §6).
3. Logout móvil que revoque el refresh en servidor (igual que el logout
   web) y borre el almacén seguro.
4. Detección de reutilización de refresh (un refresh ya rotado que
   vuelve a usarse = posible robo): revocar la familia completa y
   auditar el evento, igual que se propone en P1-A1 §2.2.
5. La sesión web por cookie sigue existiendo, intacta, para el front
   Next.js. Son dos presentaciones de la misma sesión revocable, no dos
   sistemas de autenticación distintos: el control de autorización
   (`CurrentUser`, roles, `requireCaso`) es el mismo en ambos.

**Alternativa descartada:** embeber la web en un WebView dentro de una
app y llamarlo "app móvil". Reutiliza la cookie, pero hereda todos los
problemas de la web en pantalla pequeña, no da experiencia móvil real y
no resuelve nada de la IA. No se recomienda.

### Condición 2 — CORS y origen: no usarlos como control de seguridad móvil

Las apps nativas no envían `Origin` como un navegador, así que la lista
cerrada de CORS del backend no las protege ni debe bloquearlas. Reglas:

- La lista CORS cerrada se mantiene exactamente igual para el front web
  (nunca `*` con credenciales, P2-A4).
- La autenticación móvil no depende de `Origin` ni de CORS: depende del
  token, de la autorización por firma y del rate limiting. Un `Origin`
  ausente o distinto en móvil no es, por sí solo, motivo de bloqueo ni
  de permiso.
- Si el backend tiene un filtro de verificación de origen para
  operaciones de cambio de estado (P0-A2, `CsrfOriginFilter`), debe
  distinguir el canal: cookie web (verificación de origen activa) vs.
  token móvil (sin cookie, el riesgo CSRF no aplica igual porque no hay
  cookie que el navegador adjunte automáticamente). Documentar esa
  distinción; no desactivar el filtro para la web para "que funcione el
  móvil".

### Condición 3 — Autorización: reutilizarla sin atajos

- Todos los endpoints que use la app móvil pasan por `CurrentUser` y
  `requireCaso()` exactamente igual que la web. No se crea ningún
  endpoint "para móvil" que devuelva más datos, listados sin paginar o
  casos sin vínculo autorizado.
- La app pide "mis casos" (`GET /api/casos/mis-casos`, ya existente) y
  "mi perfil" (`GET /api/auth/me`, ya existente), no casos por ids de
  terceros. Cualquier endpoint nuevo para móvil se somete a las pruebas
  P0-A4 correspondientes antes de usarse.
- Paginación (P1-B2) antes de la app: una app que trae listados
  completos a un teléfono es el mismo riesgo de fuga masiva que en web,
  con peor rendimiento.

### Condición 4 — Auditoría móvil

- Cada acción móvil se audita con las mismas acciones P1-A1, añadiendo
  un marcador de canal (`movil`) en el registro, para distinguir
  sesiones web de móviles en una investigación.
- La consulta de auditoría sigue siendo solo-admin de la propia firma;
  la app móvil no añade una pantalla de auditoría para no-admin.

### Condición 5 — Datos en el dispositivo: no copiar casos al teléfono

- La app consulta bajo demanda y no guarda casos, clientes ni documentos
  en disco del teléfono sin cifrar. Si más adelante se pide modo
  sin conexión, eso es un proyecto aparte (almacenamiento cifrado,
  borrado remoto, sincronización con conflictos) y no parte de esta
  primera app. Primera versión: siempre en línea, siempre contra la API.
- Las descargas de documentos en móvil usan la misma descarga autorizada
  con `Content-Disposition: attachment` de P0-B5; no se abren en
  visores que ejecuten contenido.

### Condición 6 — Publicación

- Beta cerrada por el mismo canal que HabitLook (APK firmado compartido
  por Drive) o tiendas, según decida Daniel; nunca con datos reales de
  clientes en la primera beta: datos ficticios hasta cerrar P0 + P1-A1.
- La app no incluye llaves de Supabase ni secretos: solo la URL pública
  de la API y el login del usuario. La llave de Supabase vive solo en el
  servidor (P0-B5).

**Orden de trabajo móvil:** 0 (P0-A4) → 1 (sesión móvil) → 2 (origen/
CSRF por canal) → 3 (endpoints reutilizados + paginación) → 4
(auditoría con canal) → 5 (sin copia local) → 6 (beta con datos
ficticios).

## 4. IA que consulte casos: veredicto por lectura

### 4.1 Lectura (i): consultar casos "por número de abogado"

**Viable solo así:** el "número de abogado" de la consulta debe ser, en
la práctica, el abogado del token. Es decir: el endpoint es "mis casos"
y el backend resuelve el abogado desde el token, no desde un parámetro
que el cliente envía. Si la IA recibe "dame los casos del abogado 123"
y 123 no es el usuario del token, la respuesta es 403 (o 404 neutro),
salvo que el solicitante sea admin de la misma firma. Aceptar un id de
abogado del cliente para filtrar sería reintroducir exactamente el IDOR
que P0-B2 cerró.

Si "número" significa tarjeta profesional: se resuelve tarjeta →
abogado en servidor, y luego se aplica la misma regla (solo el propio
abogado o admin de su firma). Si significa teléfono: no se implementa
(§1).

### 4.2 Lectura (ii): consultar por radicado / número de caso

**Viable y recomendada como primera función de IA.** El flujo es:

1. El abogado, autenticado, pide bajo demanda un caso por su radicado
   (judicial o interno — la UI debe decir cuál es cuál, P1-B8: hoy el
   radicado interno se presenta sin distinguirlo del judicial, y eso no
   debe heredarlo la IA).
2. El backend verifica firma + vínculo autorizado (`requireCaso()`).
3. Solo entonces se construye la respuesta, minimizada (ver §5).

Ya existen los endpoints de búsqueda por radicado en `CasoController`
(`buscar/radicado-interno`, `buscar/radicado-judicial`); la IA los
reutiliza, no crea una vía paralela de acceso a casos.

### 4.3 Lectura "automática" = consulta programada / periódica

**No recomendada.** Un proceso que cada hora (o cada día) toma casos y
los envía a un LLM externo multiplica la exposición (más envíos, más
copias fuera del control del despacho), genera costos sin usuario que
los pida, produce respuestas sobre datos que ya cambiaron, y deja un
rastro de auditoría difícil de explicar ante un titular ("¿por qué se
envió mi caso a un tercero si nadie lo pidió?"). Lo único programado
admisible es lo que **no** va a un LLM externo: recordatorios de
vencimientos, conteos por estado o listados de agenda, calculados dentro
del backend con datos propios. Si algún día se quiere un resumen
periódico generado por IA, debe ser: bajo demanda del abogado, sobre
casos concretos que él elija, minimizado (§5), y con su consentimiento
registrado — es decir, deja de ser "automático".

## 5. Ley 1581 y confidencialidad ante un LLM externo (Groq u otro)

Esta es la parte que más condiciona la IA, y no se resuelve con código:

1. **Nunca enviar casos completos al LLM.** Cédulas, direcciones,
   correos, teléfonos, contenido de documentos y descripción completa
   del caso NO van a un LLM externo. Lo que viaja es lo mínimo para la
   tarea: por ejemplo, para "resume el estado de este caso": título,
   tipo/subtipo, estado, fechas y un resumen redactado por el abogado —
   no el expediente.
2. **Minimización antes del envío, en el backend, no en la app.**
   La app no decide qué se envía: un endpoint del backend construye el
   extracto permitido y registra qué se envió, a qué proveedor y para
   qué caso (auditoría P1-A1). Si el extracto no se puede construir sin
   datos sensibles, la función no se ofrece para ese caso.
3. **Consentimiento y contrato.** Enviar datos de un cliente a Groq o a
   cualquier LLM externo es una transferencia a un subencargado: exige
   autorización del titular (o base legal determinada por el abogado),
   contrato de tratamiento con el proveedor, y añadirlo a la lista de
   subencargados del aviso (P1-A3a) y del contrato (P1-A3b). Sin eso,
   la función no se enciende, aunque técnicamente funcione.
4. **Sin entrenamiento con los datos.** Verificar y documentar que el
   proveedor no usa los datos enviados para entrenar modelos; si no se
   puede verificar, no se envía nada sensible (que, por §5.1, ya no se
   envía de todos modos).
5. **Sin decisiones automáticas sobre derechos.** La IA resume, busca o
   sugiere; no decide estrategias, no califica pruebas ni redacta
   escritos que se presenten sin revisión humana del abogado. El abogado
   revisa y asume lo que firma.
6. **Alternativa preferida si el despacho no quiere terceros:** modelo
   local o autohospedado, o reglas sin LLM (búsqueda por radicado,
   resúmenes a partir de campos estructurados). Es la única opción que
   elimina el problema de transferencia, y debe ofrecerse como tal.

## 6. Qué NO hacer (lista de control, móvil + IA)

- No guardar tokens en `localStorage`, en cookies legibles desde
  JavaScript ni en almacenamiento inseguro del móvil (preferencias sin
  cifrar, archivos planos, logs).
- No enviar casos completos, cédulas, direcciones ni documentos a un LLM
  externo, ni "probar" la IA con casos reales.
- No aceptar ids de abogado, firma o persona desde el cliente para
  filtrar: siempre desde el token (P0-B2).
- No abrir CORS a `*` con credenciales para "que funcione la app".
- No poner la llave `service_role` de Supabase en la app ni en código
  que llegue al dispositivo.
- No activar consultas programadas/periódicas de casos hacia un LLM.
- No presentar la IA como "abogado automático" ni sus respuestas como
  asesoría: es una ayuda de consulta, revisada por el abogado.
- No prometer que el producto "cumple la Ley 1581" por tener IA o app
  móvil: el cumplimiento se demuestra con los documentos P1-A3 y las
  pruebas P0, no con funciones nuevas.

## 7. Qué se rescata del backend actual y qué se tira

| Se rescata (sin cambios de diseño) | Por qué |
|---|---|
| Entidades `Caso`, `Cliente`, `Documento`, `Persona`, `Firma`, `Vinculo_Caso` | El modelo de datos ya refleja el dominio del despacho. |
| `CurrentUser`, `requireCaso()`, roles y `@PreAuthorize` | La autorización por firma es la pieza más valiosa; móvil e IA la heredan. |
| `RefreshTokenService` (refresh rotativo y revocable) | La sesión móvil reutiliza el mismo inventario de refresh. |
| `AuditoriaService` y `AuditoriaSeguridad` | Solo hay que añadir acciones y el marcador de canal. |
| `FileStorageService` (Supabase, servidor) y validación P0-B5 | La descarga móvil usa la misma vía autorizada. |
| Endpoints `mis-casos`, `auth/me`, búsqueda por radicado | La app y la IA los reutilizan sin vía paralela. |

| Se tira / no se reutiliza | Por qué |
|---|---|
| La sesión por cookie httpOnly, como vía para móvil | Las apps nativas no comparten cookies con el navegador igual que la web (§3, condición 1). |
| Las suposiciones de CORS/Origen del front web | No aplican a apps nativas (§3, condición 2). |
| El código del front Next.js | No se reutiliza en una app nativa (solo el contrato de la API). |
| Cualquier diseño de "copia local de casos en el teléfono" | Fuera del alcance de la primera versión (§3, condición 5). |
| La idea de IA automática periódica con casos completos | Rechazada en §4.3 y §5. |

## 8. Orden recomendado (móvil + IA juntos)

1. Cerrar P0-A4 (pruebas contra API real con Postgres).
2. P1-A1 (auditoría completa) + P1-B2 (paginación) — prerrequisitos de
   cualquier cliente nuevo.
3. Sesión móvil (condición 1) + distinción CSRF por canal (condición 2).
4. App móvil v1: login, mis casos, detalle por radicado, documentos —
   todo reutilizando endpoints, sin copia local, beta con datos ficticios.
5. Solo entonces, IA v1 bajo demanda y minimizada: consulta por radicado
   y resumen de estado a partir de campos estructurados, con el
   proveedor añadido como subencargado y consentimiento documentado.
6. Decisión posterior, separada: si se justifica, modelo local o
   autohospedado para eliminar la transferencia a terceros; nunca
   consultas programadas de casos completos a un LLM externo.
