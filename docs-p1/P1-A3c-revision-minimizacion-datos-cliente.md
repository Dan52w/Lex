# P1-A3c — Revisión de minimización de datos del cliente (LexCloud)

**Fase:** P1-A3 del Plan LexCloud P0/P1/P2. **Estado:** documento de
revisión. **No borra ni cambia ningún campo del código:** solo clasifica
cada campo y recomienda qué hacer con él. Cualquier cambio de modelo o
de formulario es trabajo futuro de implementación, con migración
versionada (P1-B1) si toca la base de datos.
**Base verificada:** entidad `Cliente` (`entidades/Cliente.java`), tabla
`clientes` y catálogos en `LexCloud_DB_completa_P0.sql`, DTO
`ClienteRequestDTO` y formulario `NuevoClienteModal.tsx` del front.
**Regla aplicada:** un campo se queda obligatorio solo si la gestión del
caso lo exige; si su uso es eventual o estadístico, es opcional; si no
tiene finalidad documentada para un despacho, es candidato a quitar.
**Este documento no es asesoría legal.**

## 1. Campos de identificación y gestión del caso (núcleo)

| Campo (entidad / SQL) | Obligatoriedad hoy | Veredicto | Finalidad justificada |
|---|---|---|---|
| `firma` (`id_firma`, NOT NULL) | Obligatorio | **Conservar obligatorio** | Aislamiento por firma: es la base de toda la autorización P0-B2. Sin firma no hay caso. |
| `tipoCliente` (`id_tipo_cliente`, NOT NULL; Natural/Jurídico) | Obligatorio | **Conservar obligatorio** | Cambia qué datos exige el caso (persona natural vs. empresa) y qué documentos aplican. |
| `identificacion` + `tipoIdentificacion` (CC, CE, Pasaporte, NIT) | Obligatorio en DTO y SQL | **Conservar obligatorio** | Identificar al cliente ante juzgados y en radicados; unicidad por firma (`identificacion, id_firma`). |
| `nombres`, `primerApellido` | Obligatorios | **Conservar obligatorios** | Identificación mínima en escritos y notificaciones. |
| `segundoApellido` | Opcional | **Conservar opcional** | Parte del nombre legal; muchos clientes lo tienen, otros no. Nunca obligatorio. |
| `fechaVinculacion` | Automática | **Conservar** | Trazabilidad de desde cuándo es cliente de la firma. |
| `activo` (borrado lógico) | Interno | **Conservar** | Borrado lógico coherente con P1-B7; no es dato del titular. |

## 2. Contacto y ubicación

| Campo | Veredicto | Finalidad / nota |
|---|---|---|
| `correo` (NOT NULL en SQL y DTO) | **Conservar obligatorio** | Canal de notificación al cliente. Verificar formato (P1-B3); no usarlo como identificador de acceso. |
| `telefonoMovil` (obligatorio en DTO) | **Conservar obligatorio** | Contacto operativo del despacho. Un solo teléfono obligatorio basta. |
| `telefonoResidencia`, `telefonoOficina` | **Opcionales** | Útiles si el cliente prefiere otro canal; nunca los tres obligatorios. |
| `direccionResidencia` | **Conservar opcional con justificación** | Necesaria cuando el caso exige notificaciones físicas o domicilio procesal. Si el caso es 100% digital, puede quedar vacía. |
| `direccionOficina` | **Opcional** | Solo para clientes con domicilio laboral relevante al caso. |
| `ciudadResidencia` | **Conservar opcional** | Ubicación general para competencia territorial y contacto. Menos sensible que la dirección exacta. |
| `localidadSector`, `barrio` | **Candidatos a quitar o fusionar en opcionales** | Detalle de ubicación fina sin finalidad procesal clara en la mayoría de casos. Si el despacho no documenta para qué los usa (p. ej., caracterización para un programa de acceso a la justicia), quitarlos del formulario y dejar solo `ciudadResidencia` + `direccionResidencia` cuando aplique. |

## 3. Datos personales adicionales

| Campo | Veredicto | Finalidad / nota |
|---|---|---|
| `fechaNacimiento` (obligatoria en DTO) | **Bajar a opcional, salvo casos que lo exijan** | Sirve para verificar capacidad legal, edad y algunos trámites (familia, laboral, pensional). Pero no todo caso la necesita: hacerla obligatoria en todos los casos viola minimización. Recomendación: opcional por defecto; obligatoria solo en tipos de caso que el despacho liste (familia, laboral, sucesiones). |
| `lugarNacimiento` | **Candidato a quitar** | Casi ningún trámite de caso lo exige si ya existe la identificación. Conservar solo si el despacho documenta un tipo de caso que lo pida (p. ej., trámites de nacionalidad o migración). |
| `nacionalidad` | **Opcional** | Relevante en casos de extranjería, migración o cuando la identificación es pasaporte/CE. En casos ordinarios de nacionales, innecesaria. |
| `genero` (Masculino, Femenino, No Binario, Prefiero no decirlo) | **Opcional, nunca obligatorio** | Solo con finalidad (trato adecuado, estadísticas del despacho, enfoque diferencial). El catálogo ya incluye "Prefiero no decirlo": mantenerlo y no forzar respuesta. |
| `estadoCivil` (Soltero, Casado, Unión Libre, Divorciado, Viudo) | **Opcional, por tipo de caso** | Imprescindible en familia, sucesiones y algunos civiles; irrelevante en la mayoría de laborales o penales. Opcional por defecto; obligatorio solo en los tipos de caso que el despacho liste. |

## 4. Caracterización socioeconómica (el bloque a revisar con más rigor)

Estos seis campos vienen de catálogos (`estratos_economicos`,
`tipo_viviendas`, `niveles_salariales`, `tipo_empleados`,
`grados_escolaridad`, `discapacidades`) y se piden en el formulario
actual. Ninguno es necesario para abrir, seguir o documentar un caso
ordinario. Su única justificación posible es una finalidad específica
del despacho, documentada por escrito. Sin esa finalidad, sobran.

| Campo | Valores actuales | Veredicto | Finalidad que lo justificaría (si existe) |
|---|---|---|---|
| `estrato` (Estrato 1–6) | Catálogo | **Opcional; candidato a quitar** | Amparo de pobreza, beneficios de acceso a la justicia, o caracterización para programas sociales del despacho. Si el despacho no presta esos servicios, quitarlo. |
| `vivienda` (Propia, Arrendada, Familiar, Compartida) | Catálogo | **Opcional; candidato a quitar** | Misma justificación que estrato (capacidad económica, embargos sobre vivienda). Sin esa finalidad, quitarlo. |
| `nivelSalarial` (Menos de 1 SMLV … Más de 5 SMLV) | Catálogo | **Opcional; candidato a quitar** | Alimentos, regulación de honorarios según capacidad, o amparo de pobreza. Dato económico sensible en la práctica: si se conserva, debe ser opcional y con acceso restringido en la UI. |
| `tipoEmpleado` (Independiente, Dependiente, Desempleado, Estudiante, Pensionado) | Catálogo | **Opcional; candidato a quitar** | Laboral y pensional (tipo de vinculación sí importa ahí); en el resto de casos, no. Opcional por defecto. |
| `gradoEscolaridad` (Primaria … Postgrado) | Catálogo | **Candidato a quitar** | La finalidad procesal más débil de todo el bloque. Solo se justificaría en estudios o caracterización con fin documentado. Recomendación: quitar del formulario salvo justificación escrita del despacho. |
| `discapacidad` (Ninguna, Física/Motora, Visual, Auditiva, Cognitiva/Mental, Múltiple) | Catálogo | **Dato sensible: opcional estricto o quitar** | Es un dato de salud bajo la Ley 1581: exige autorización expresa del titular, finalidad concreta (ajustes razonables de atención, enfoque diferencial, beneficios procesales) y acceso restringido. Si el despacho no puede documentar esa finalidad y esa autorización, **quitarlo**: es el campo de mayor riesgo de todo el modelo. Si se conserva, debe ser opcional, nunca obligatorio, y no debe viajar a ningún servicio externo (ver veredicto IA). |

## 5. Recomendación resumida para Daniel y el despacho

1. **Quitar del formulario (salvo justificación escrita):**
   `gradoEscolaridad`, `lugarNacimiento`, `localidadSector`, `barrio`.
2. **Dejar opcionales (nunca obligatorios):** `segundoApellido`,
   `fechaNacimiento` (salvo tipos de caso listados), `nacionalidad`,
   `genero`, `estadoCivil`, `telefonoResidencia`, `telefonoOficina`,
   `direccionOficina`, `estrato`, `vivienda`, `nivelSalarial`,
   `tipoEmpleado`.
3. **Decisión expresa sobre `discapacidad`:** quitar, o conservar como
   opcional estricto con autorización expresa, finalidad documentada y
   acceso restringido. No hay tercera opción silenciosa.
4. **Núcleo obligatorio que queda:** firma, tipo de cliente,
   identificación, nombres, primer apellido, correo y teléfono móvil.
5. **Antes de quitar un campo del modelo:** confirmar que ningún informe,
   formato judicial o integración del despacho lo usa; el borrado de
   columna es migración versionada (P1-B1), no edición a mano, y los
   datos históricos se tratan según el contrato de encargo (P1-A3b §9).
6. **Coherencia DTO ↔ entidad ↔ SQL:** hoy el DTO exige
   `fechaNacimiento` y `telefonoMovil` mientras varias columnas son
   opcionales en la entidad. Al aplicar esta revisión, alinear los tres
   lados para no repetir el desajuste ya señalado en P1-B3 (caso de
   `Documento.descripcion`).

## 6. Pendiente de Daniel

- Confirmar con cada despacho piloto qué campos del §4 realmente usa y
  con qué finalidad, y firmar esa lista. Lo no firmado se quita.
- Decisión sobre `discapacidad` (§4) con abogado, antes de cualquier
  uso con datos reales.
