# P1-A3b — Contrato / anexo de encargo de tratamiento (LexCloud)

**Fase:** P1-A3 del Plan LexCloud P0/P1/P2. **Estado:** borrador para
validación por abogado. **No usar con clientes reales sin esa validación.**
**Este documento no es asesoría legal.**
**Nota:** Akmon Studios no está legalizada: no se la presenta aquí como
empresa constituida. Esa condición debe quedar explícita en el contrato
firmado; no se firma como si fuera una sociedad constituida.

> Los marcadores [A COMPLETAR] deben completarse con datos reales antes
> de firmar. Un contrato con marcadores vacíos no está listo.

---

## Anexo de encargo de tratamiento de datos personales — LexCloud

**Entre:**
- **Responsable:** el despacho de abogados cliente:
  [NOMBRE O RAZÓN SOCIAL A COMPLETAR], [IDENTIFICACIÓN A COMPLETAR],
  [DIRECCIÓN A COMPLETAR], en adelante "el Despacho".
- **Encargado:** quien opera la plataforma LexCloud:
  [NOMBRE/IDENTIFICACIÓN A COMPLETAR POR AKMON O POR EL OPERADOR],
  [DIRECCIÓN A COMPLETAR], en adelante "el Encargado".

**Considerando** que el Despacho contrata LexCloud para gestionar sus
casos y que para ello el Encargado tratará datos personales de los
clientes del Despacho, las partes acuerdan:

### 1. Objeto y finalidad

El Encargado tratará los datos personales **únicamente** para prestar
el servicio LexCloud al Despacho: gestión de casos, clientes, agenda,
documentos y usuarios del Despacho, con las medidas de seguridad de este
anexo. Queda prohibido cualquier otro uso, incluida la publicidad, la
venta, la cesión a terceros y el entrenamiento de modelos de
inteligencia artificial, salvo autorización escrita y específica del
Despacho (ver §5).

### 2. Datos y titulares

- **Titulares:** clientes del Despacho y usuarios del Despacho
  (abogados, asistentes y administradores).
- **Categorías:** identificación y contacto, datos del caso, documentos
  del caso, datos sociodemográficos opcionales (ver P1-A3c) y registros
  de auditoría de seguridad. La discapacidad es dato sensible (salud):
  solo se trata si el Despacho la registra con autorización expresa del
  titular y finalidad documentada.
- El Encargado no recoge datos directamente de los titulares: los recibe
  del Despacho, que garantiza contar con la autorización de los
  titulares o con otra base legal válida, según determine su abogado.

### 3. Instrucciones y confidencialidad

- El Encargado actúa solo según las instrucciones documentadas del
  Despacho. Si una instrucción contradice la Ley 1581 de 2012, lo
  advierte por escrito y suspende su ejecución hasta aclararla.
- El personal del Encargado con acceso a los datos está obligado a
  confidencialidad, y el acceso se limita a lo necesario para operar el
  servicio (principio de mínimo acceso). El acceso de soporte queda
  registrado en la auditoría cuando sea técnicamente posible.

### 4. Medidas de seguridad (resumen; detalle operativo en P1-A2)

El Encargado mantiene, como mínimo: acceso por firma desde el token
(ningún usuario ve datos de otra firma), sesión en cookie httpOnly con
refresh revocable, control por rol (solo admin gestiona usuarios y
auditoría), auditoría de seguridad inmutable desde la UI y consultable
solo por el admin de la firma, cifrado en tránsito (HTTPS) y cifrado en
reposo verificado en el proveedor, y backups con prueba de restauración.
Ante un cambio que reduzca estas medidas, el Encargado avisa al Despacho
antes de aplicarlo.

### 5. Subencargados

Subencargados actuales (completar antes de firmar; la lista del aviso
P1-A3a debe coincidir con esta):

| Subencargado | Función |
|---|---|
| [PROVEEDOR DE BASE DE DATOS Y ALMACENAMIENTO — p. ej. Supabase, proyecto nuevo, A COMPLETAR] | Base de datos y documentos |
| [PROVEEDOR DE HOSTING — A COMPLETAR] | Backend y frontend |
| [OTRO — A COMPLETAR O "NINGUNO"] | [FUNCIÓN] |

El Encargado no añade subencargados sin avisar al Despacho con
antelación razonable y sin contrato equivalente con cada subencargado.
El Encargado sigue siendo responsable ante el Despacho por lo que hagan
sus subencargados. **No se incluye ningún servicio externo de
inteligencia artificial como subencargado** salvo que se añada
expresamente por escrito, con su finalidad, su minimización de datos y
su contrato.

### 6. Registro Nacional de Bases de Datos (RNBD) ante la SIC

**Decisión pendiente, no asumida.** Corresponde al abogado del Despacho
(y al del Encargado, para su propio rol) determinar si el Despacho, el
Encargado, o ambos deben inscribir bases de datos en el RNBD ante la
Superintendencia de Industria y Comercio, y con qué contenido. Este
anexo no afirma que el registro aplique ni que no aplique, y no fija
plazos. La decisión debe quedar documentada aquí antes de tratar datos
reales:

- Decisión para el Despacho: [REGISTRAR / NO REGISTRAR — MOTIVO Y FECHA,
  A COMPLETAR CON ABOGADO].
- Decisión para el Encargado: [REGISTRAR / NO REGISTRAR — MOTIVO Y FECHA,
  A COMPLETAR CON ABOGADO].

### 7. Derechos de los titulares y cooperación

- El Despacho atiende las solicitudes de los titulares (consulta,
  rectificación, supresión, revocación). El Encargado coopera sin demora:
  entrega, corrige o suprime los datos que le indique el Despacho por
  escrito, dentro del plazo que fije el Despacho conforme a la Ley 1581.
- El Encargado avisa al Despacho de inmediato si recibe directamente
  una solicitud de un titular, y no la responde por su cuenta.

### 8. Incidentes de seguridad

El Encargado avisa al Despacho **sin dilación indebida** cuando conozca
un incidente que afecte datos del Despacho, con el alcance conocido, las
medidas de contención y la evidencia disponible (ver el plan de
respuesta a incidentes, P1-A2 §3). La decisión de avisar a los titulares
y a la SIC la toma el Despacho con su abogado; el Encargado le entrega
la información necesaria para decidirla y deja constancia de la
decisión.

### 9. Devolución y borrado al terminar la relación

Al terminar el contrato, a elección del Despacho y por escrito:

1. **Devolución:** el Encargado entrega al Despacho una exportación de
   sus datos (base de datos y documentos) en formato utilizable, dentro
   de [PLAZO A COMPLETAR] días; o
2. **Borrado:** el Encargado borra los datos del Despacho de sus
   sistemas y de los subencargados, y entrega una certificación de
   borrado.

Los backups se depuran en el siguiente ciclo de rotación
([CICLO A COMPLETAR]) y eso queda escrito aquí, no prometido como
borrado instantáneo. Los registros de auditoría se conservan el plazo
que fije el Despacho con su abogado para atender reclamaciones, y luego
se borran con acta. La revocación de accesos del Despacho es inmediata
al terminar la relación.

### 10. Duración, responsabilidad y ley aplicable

- Duración: la del contrato principal de prestación del servicio
  LexCloud.
- El Encargado responde ante el Despacho por incumplir este anexo.
  [LÍMITES DE RESPONSABILIDAD Y PENALIDADES, A COMPLETAR CON ABOGADO —
  no se fijan aquí cifras inventadas].
- Ley aplicable: Ley 1581 de 2012, Decreto 1377 de 2013 y demás normas
  colombianas de protección de datos vigentes, según determine el
  abogado.

### 11. Checklist antes de firmar

- [ ] Marcadores completos (§1, §5, §6, §9, §10) y lista de
      subencargados idéntica a la del aviso P1-A3a.
- [ ] Decisión RNBD documentada (§6), no asumida.
- [ ] Condición legal del Encargado declarada con exactitud (proyecto
      no constituido, si sigue siendo el caso).
- [ ] Revisión y firma por abogado del Despacho y del Encargado.
- [ ] Fecha de firma: [FECHA].
