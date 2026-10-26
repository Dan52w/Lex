# Documentos P1 de LexCloud — índice

Paquete de documentación P1 (solo documentos; no se tocó código backend
ni frontend, no se hizo push, no se borraron ramas, no se reveló ni
guardó ningún secreto, no se tocó `application.properties`).
Plan base: `~/workspace/your_files/Plan_LexCloud_P0_P1_P2.md` (P1-A1, P1-A2,
P1-A3 y P2-A4 "qué NO hacer"). Estado general: **P0 implementado y subido
(backend `0c880c90`, front `b5806072`), pero NO cerrado para uso real:
faltan las 12 pruebas A4 de integración con Postgres en el PC de Daniel.**
Este paquete no es asesoría legal. Akmon Studios no está legalizada.

| Documento | Tarea | Contenido |
|---|---|---|
| `P1-A1-auditoria-completa.md` | P1-A1 | Eventos a auditar además del mínimo P0 (casos, clientes, estados, subida y papelera de documentos, agenda, equipo, logout, refresh, accesos denegados), campos (autor del token, firma, recurso, fecha, resultado, IP), consulta solo-admin de su firma, inmutabilidad desde la UI, y 10 puntos de trabajo en código pendiente. |
| `P1-A2-operacion-backups-cifrado-incidentes.md` | P1-A2 | Backups de BD + documentos con prueba de restauración mensual, HTTPS, verificación de cifrado en reposo/SSL/bucket privado en el proyecto Supabase nuevo, plan de respuesta a incidentes (quién decide, contención, avisos, SIC si aplica) y revisión de logs sin datos personales, con comandos PowerShell/psql y marcadores (sin valores reales). |
| `P1-A3a-aviso-privacidad-politica-tratamiento.md` | P1-A3 | Borrador de aviso de privacidad y política de tratamiento: datos, finalidad, responsable/encargado con marcadores, subencargados con marcadores, conservación, derechos (consulta, rectificación, supresión) y nota de validación por abogado antes de uso real. |
| `P1-A3b-contrato-encargo-tratamiento.md` | P1-A3 | Borrador de contrato/anexo de encargo: finalidad, medidas, subencargados, devolución/borrado al terminar, y RNBD ante la SIC como decisión documentada pendiente de abogado (no asumida). |
| `P1-A3c-revision-minimizacion-datos-cliente.md` | P1-A3 | Campo por campo del cliente (entidad `Cliente` + SQL): núcleo obligatorio, opcionales, candidatos a quitar (`gradoEscolaridad`, `lugarNacimiento`, `localidadSector`, `barrio`) y decisión expresa sobre `discapacidad` (dato sensible de salud). Sin borrar campos del código. |
| `VEREDICTO-movil-ia.md` | Móvil + IA | Veredicto: **viable con cambios previos, sin reestructurar desde cero**; app móvil con sesión nueva para nativo (la cookie httpOnly no se comparte igual), CORS sin usar como control móvil, autorización y auditoría reutilizadas; IA por radicado y por abogado-del-token viable bajo demanda y minimizada; IA "automática" periódica con casos completos a un LLM externo, rechazada. Qué se rescata y qué se tira del backend actual, y qué NO hacer. |

## Pendiente de Daniel (resumen)

- Validar P1-A3a y P1-A3b con un abogado y completar todos los marcadores
  antes de cualquier uso con clientes reales.
- Decidir y documentar el RNBD ante la SIC (P1-A3b §6) y la notificación
  a la SIC en incidentes (P1-A2 §3): ninguna de las dos se asume.
- Decidir sobre `discapacidad` y los campos sociodemográficos (P1-A3c §6)
  con cada despacho piloto.
- Cerrar el camino `findAll()` de auditoría para admin sin firma o crear
  el rol de operador interno (P1-A1 §3).
- Verificar y anotar con fecha el cifrado en reposo, SSL, bucket privado
  y backups del proyecto Supabase nuevo (P1-A2 §2.2).
- Ejecutar las 12 pruebas P0-A4 en su PC: sin ellas, nada de este paquete
  habilita uso con datos reales.
