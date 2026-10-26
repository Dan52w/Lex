# P1-A2 — Operación: backups, cifrado y respuesta a incidentes (LexCloud)

**Fase:** P1-A2 del Plan LexCloud P0/P1/P2. **Estado:** documento operativo.
**No es código ni cambia configuración.** Son pasos para el PC y el despliegue
de Daniel, con comandos de ejemplo y marcadores en lugar de valores reales.
Nunca pegues contraseñas, URLs con credenciales ni llaves en un chat, un
documento compartido ni un commit.
**Nota:** este plan no es asesoría legal. Akmon Studios no está legalizada:
no se la presenta aquí como empresa constituida.

## 1. Backups: qué, cómo y con qué frecuencia

### 1.1 Qué se respalda (las dos mitades, siempre juntas)

1. **Base de datos PostgreSQL:** casos, clientes, personas, vínculos,
   historial, catálogos, `refresh_tokens` y `auditoria_seguridad`.
2. **Documentos:** los archivos que hoy viven en Supabase Storage
   (el backend los sirve vía `FileStorageServiceImpl` contra la API de
   Supabase). Respaldar solo la base de datos deja casos que apuntan a
   archivos inexistentes; respaldar solo los archivos deja archivos sin
   caso. Un backup útil es el par (base + archivos) con la misma fecha.

No forman parte del backup: la carpeta `.next` del front, `node_modules`,
ni el código (eso vive en Git). Sí forma parte: la lista de variables de
entorno necesarias, **sin sus valores** (ver P0-B8).

### 1.2 Backup de la base de datos (local, PC Windows con PowerShell)

El contenedor local de Postgres de Daniel se llama `storystreets-db` para
otro proyecto; no confundir. Para LexCloud, adapta los nombres entre
`<...>` a tu contenedor/base reales:

```powershell
# Copia lógica completa (recomendada para restaurar en otra máquina):
docker exec <contenedor-postgres-lexcloud> pg_dump -U <usuario> -d <base_lexcloud> -F c -f /tmp/lexcloud-backup-YYYYMMDD.dump
docker cp <contenedor-postgres-lexcloud>:/tmp/lexcloud-backup-YYYYMMDD.dump "C:\Backups\LexCloud\"
```

O, si Postgres corre instalado en local (sin Docker):

```powershell
pg_dump -U <usuario> -d <base_lexcloud> -F c -f "C:\Backups\LexCloud\lexcloud-backup-YYYYMMDD.dump"
```

Si la base vive en el proyecto Supabase nuevo (ver §3.2), el respaldo se
hace desde el panel de Supabase (Database → Backups, según el plan) y/o
con `pg_dump` contra la cadena de conexión del proyecto, guardada solo en
el gestor de secretos del entorno, nunca en el repo:

```powershell
# La cadena va por variable de entorno, nunca escrita en el comando ni en el historial:
$env:PGPASSWORD = "<contraseña-solo-en-tu-entorno>"
pg_dump "<cadena-de-conexion-supabase-sin-escribir-aqui>" -F c -f "C:\Backups\LexCloud\lexcloud-supabase-YYYYMMDD.dump"
Remove-Item Env:PGPASSWORD
```

Frecuencia mínima: **diaria automática** en producción; antes de cada
migración de esquema y antes de cada despliegue. Conserva al menos:
diarios de 7 días, semanales de 4 semanas y uno mensual. Cifra la carpeta
`C:\Backups\LexCloud\` (BitLocker o equivalente) porque contiene cédulas,
casos y documentos: un backup sin cifrar es una fuga en potencia.

### 1.3 Backup de los documentos (Supabase Storage)

1. En el proyecto Supabase nuevo, verifica que el bucket de documentos sea
   **privado** (P0-B5: si es público o la llave es `service_role`, rotar
   la llave y pasar a privado con URLs firmadas antes de cualquier uso real).
2. Programa una copia del contenido del bucket a un almacenamiento
   separado (segundo bucket o disco cifrado local) con la misma fecha que
   el `pg_dump`. La herramienta depende del proveedor: usa la CLI/API de
   Supabase o la función de respaldo del panel; lo obligatorio es que la
   copia exista, tenga fecha y se registre en la bitácora (§1.5).
3. No guardes en el backup la llave `service_role` ni ninguna credencial:
   el backup de archivos no necesita credenciales dentro.

### 1.4 Prueba de restauración (obligatoria: un backup nunca restaurado no cuenta)

Cada mes, y siempre después de cambiar de proveedor o de esquema, ejecuta
esta prueba en un entorno **separado** (otra base y otro bucket de prueba,
nunca sobre la base de producción):

```powershell
# 1. Crear base de prueba vacía y restaurar la copia:
createdb -U <usuario> lexcloud_restauracion_prueba
pg_restore -U <usuario> -d lexcloud_restauracion_prueba "C:\Backups\LexCloud\lexcloud-backup-YYYYMMDD.dump"

# 2. Levantar el backend contra esa base de prueba (variables de entorno de prueba)
#    y verificar: login con un usuario de prueba, un caso abre, un cliente abre,
#    un documento descarga y la auditoría muestra registros.
# 3. Si algo falla, el backup NO cuenta: corrige el procedimiento y repite.
# 4. Borra la base de prueba al terminar: contiene datos reales.
```

```sql
-- Verificación rápida en psql sobre la base restaurada (conteos, no contenido):
SELECT count(*) FROM casos;
SELECT count(*) FROM clientes;
SELECT count(*) FROM documentos;
SELECT count(*) FROM auditoria_seguridad;
```

Registra cada prueba: fecha, archivo restaurado, resultado y quién la hizo.
Si la restauración no se probó, en el documento de seguridad para clientes
(P2-A2) se escribe "sin prueba de restauración", no "backups verificados".

### 1.5 Bitácora de backups (documento interno, no en el repo público)

| Fecha | Tipo (BD / documentos) | Archivo | Restauración probada (sí/no + fecha) | Responsable |
|---|---|---|---|---|
| YYYY-MM-DD | BD + documentos | `lexcloud-backup-YYYYMMDD.dump` | Pendiente | [NOMBRE] |

## 2. Cifrado

### 2.1 En tránsito (HTTPS): qué verificar

- **Todo el tráfico en producción es HTTPS, sin excepción:** front, API y
  Supabase. Nada de `http://` con credenciales ni con datos de casos.
- Las cookies de sesión `lex_access` y `lex_refresh` deben ser
  `Secure=true` y `SameSite=None` en despliegue cross-site (front en
  Vercel, back en Render), o `Lax` si front y back comparten dominio.
  En local se usa `Lax` + `Secure=false` (ver `application-example.properties`
  y el documento local corregido de Daniel). Verificar con las herramientas
  del navegador: pestaña de red, cabecera `Set-Cookie`.
- HSTS y demás cabeceras son P1-B4/P1-F2 (código futuro); la verificación
  operativa aquí es: abrir el sitio y la API por `https://` y confirmar
  que no hay contenido mixto ni redirecciones a `http://`.
- En local, no hace falta certificado para probar contra `localhost`,
  pero nunca se expone el backend local a internet sin HTTPS y sin
  autenticación.

### 2.2 En reposo: qué debe verificar/activar Daniel en su proveedor

Daniel va a crear un **proyecto Supabase nuevo** y sembrar desde cero
(decisión ya tomada; el Supabase viejo y las cuentas antiguas se ignoran).
Antes de sembrar datos reales, verificar y dejar constancia escrita de
cada punto (captura o nota con fecha; no basta "el proveedor lo hace"):

| Capa | Qué verificar en Supabase / Postgres | Dónde |
|---|---|---|
| Base de datos | Cifrado en reposo activado por el proveedor (Supabase cifra los volúmenes de Postgres con cifrado gestionado por el proveedor; confirmar en la documentación/panel del proyecto nuevo y anotar la fecha de verificación) | Panel de Supabase → proyecto nuevo → Database |
| Conexión a la base | Conexión con SSL exigida (no aceptar conexiones sin SSL desde fuera de la red del proveedor) | Cadena de conexión del proyecto; `sslmode=require` o superior donde aplique |
| Storage | Bucket de documentos **privado**; acceso solo con llave del servidor y URLs firmadas de corta duración; nunca llave `service_role` en el front | Panel de Supabase → Storage → bucket |
| Llaves | La llave que usa el backend es la adecuada para servidor (no la llave pública `anon` para operaciones de escritura/borrado); rotar si hay duda de exposición | Panel de Supabase → Settings → API |
| Backups del proveedor | Backups automáticos del proyecto activados (según el plan contratado) y retención conocida; estos **no sustituyen** la prueba de restauración propia (§1.4) | Panel de Supabase → Database → Backups |
| Discos locales | BitLocker (Windows) activado en el PC donde se guardan copias locales y en cualquier disco con dumps | Configuración de Windows |
| Secretos | Ninguna credencial en el repo, en `.env` commiteado ni en la carpeta de backups; secretos solo en variables de entorno del despliegue | Revisión P0-B8 |

Si el proveedor no ofrece cifrado en reposo para algún componente, se
documenta como limitación conocida y se decide con el despacho (y con
abogado) si se acepta o se cambia de componente. No se promete cifrado
que no se verificó.

## 3. Plan básico de respuesta a incidentes (documento interno)

Guardarlo como documento interno del despacho, no en el repo público.
Roles (con nombres reales cuando el despacho los designe):

- **Quien decide:** el responsable del despacho cliente (responsable del
  tratamiento, ver P1-A3a) decide los avisos a afectados y a la SIC.
  Akmon Studios, como encargado, ejecuta la contención técnica y le
  entrega la evidencia; no decide sola sobre avisos legales.
- **Quien ejecuta la contención técnica:** [NOMBRE / ROL A COMPLETAR POR
  EL DESPACHO Y AKMON] — revocar sesiones, rotar llaves, cerrar accesos.
- **Quien atiende a los titulares:** [NOMBRE / ROL A COMPLETAR] — responde
  consultas, rectificaciones y reclamos derivados del incidente.

### Pasos, en orden

1. **Detectar y registrar.** Fecha y hora (America/Bogota), quién lo
   detectó, qué se observó (ej.: registros `ACCESO_DENEGADO` repetidos,
   descarga inusual, aviso de un usuario). Abrir el registro del
   incidente (§3.1). No borrar evidencia: conservar logs y auditoría.
2. **Contener (primeras horas).**
   - Revocar las sesiones afectadas (revocar refresh tokens; desactivar
     la persona si aplica — P0-B3/B6 ya lo soporta).
   - Rotar las credenciales sospechosas (llave de Supabase, secreto JWT)
     y cerrar el acceso indebido (cambiar la llave, no solo borrar un
     archivo).
   - Si la fuga es de documentos, revocar las URLs firmadas vigentes y
     cambiar la llave del bucket.
   - No avisar al presunto atacante ni discutir el incidente en canales
     públicos.
3. **Evaluar el alcance.** Con la auditoría (§P1-A1) y los logs: qué
   firmas, casos, clientes y documentos se vieron, descargaron o
   borraron; desde cuándo; si hubo exfiltración o solo acceso.
4. **Avisar.**
   - Al despacho afectado, de inmediato y por escrito, con el alcance
     conocido hasta el momento.
   - A los titulares afectados, cuando el despacho (con su abogado) lo
     determine: qué pasó, qué datos, qué se hizo y qué pueden hacer.
   - A la **SIC (Superintendencia de Industria y Comercio) si aplica**:
     la decisión de notificar, el plazo y el contenido los determina el
     despacho con su abogado según la Ley 1581 de 2012 y sus decretos;
     este documento no asume que siempre aplica ni fija un plazo. Lo
     que sí es obligatorio: dejar la decisión documentada (notificar o
     no, con el motivo), no dejarla en el aire.
5. **Recuperar.** Restaurar desde backup probado si hubo borrado o
   corrupción (§1.4); verificar que la causa quedó cerrada (pruebas P0
   pertinentes en verde) antes de reabrir el acceso.
6. **Cerrar y aprender.** Informe final con causa raíz, medidas tomadas
   y medidas preventivas; actualizar este plan y la auditoría si el
   incidente reveló un hueco. Conservar el expediente del incidente
   según la retención que defina el despacho con su abogado.

### 3.1 Registro mínimo de cada incidente

Fecha/hora de detección y de contención, descripción, sistemas afectados,
datos afectados (categorías, no listados de cédulas), sesiones/llaves
revocadas, personas avisadas (despacho, titulares, SIC) con fecha, y
decisión documentada sobre la notificación a la SIC.

## 4. Revisión de logs sin datos personales

Regla: **un log nunca contiene cédulas, correos, tokens, contraseñas ni
contenido de documentos o de casos.** El `printStackTrace` de
`JwtUtil.validateJwtToken()` ya se quitó en P0-B6; esta revisión cubre
el resto.

### 4.1 Qué buscar (PowerShell / búsqueda en el código)

```powershell
# Buscar impresiones sospechosas en el backend (ejecutar en la carpeta del repo):
Select-String -Path "src\main\java\**\*.java" -Pattern "System\.out\.println|printStackTrace|log\.(info|debug|error).*correo|log\..*identificacion|log\..*token" | Select-Object Filename, LineNumber, Line
```

Patrones prohibidos en cualquier log: el valor de una cédula o de un
correo, el contenido de un `Authorization` o de una cookie, el cuerpo
de un documento, la descripción completa de un caso, una contraseña
(incluso la intentada en un login fallido) y la cadena de conexión a la
base de datos.

### 4.2 Qué sí puede ir en un log

Identificadores internos opacos (id numérico de caso/cliente/persona),
acción, resultado (éxito/fallo), fecha, y mensajes de error técnicos sin
datos personales (ej.: "fallo de conexión a la base", sin la cadena de
conexión). Los errores al cliente siguen siendo neutros (P1-F3): el
detalle vive solo en el log interno, y el log interno no vive en el repo
ni se comparte con terceros.

### 4.3 Revisión periódica

Cada despliegue y cada mes: revisar una muestra de logs del backend y
del front buscando los patrones del §4.1; si aparece un dato personal,
se corrige el punto de log, se rota el secreto si era un token, y se
registra en la bitácora. Los logs antiguos que contengan datos
personales se depuran según la retención definida (no se conservan
"por si acaso").

## 5. Checklist de cierre de P1-A2

- [ ] Backup diario automático de BD + documentos, con bitácora (§1.5).
- [ ] Prueba de restauración mensual documentada; la última, en verde.
- [ ] HTTPS verificado en front, API y Supabase; cookies `Secure` en
      producción.
- [ ] Cifrado en reposo, bucket privado, SSL y backups del proveedor
      verificados y anotados con fecha para el proyecto Supabase nuevo.
- [ ] Plan de respuesta a incidentes con nombres, pasos y registro (§3),
      guardado como documento interno.
- [ ] Revisión de logs sin datos personales ejecutada y sin hallazgos
      pendientes.
