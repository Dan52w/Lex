-- Flyway V3: alinear el esquema con las entidades Java actuales.
-- El SQL original es más viejo que las entidades: faltaban columnas que el
-- backend ya usa (tarjeta_profesional, radicados, id_firma, id_abogado) y
-- dos tablas tenían nombres distintos a las entidades.
-- Todo es condicional (DO blocks / IF NOT EXISTS): aplicable a base nueva
-- (donde V1 ya creó parte) y a base existente (donde V1 se marcó como
-- baseline sin ejecutarse). NO borra datos ni columnas.
-- La columna vieja agendas.id_persona se CONSERVA como obsoleta en P1:
-- no se borra hasta verificar que ningún dato depende de ella; el código
-- nuevo usa id_abogado. Ver README, sección Migraciones.

-- Renombres de catálogos (PostgreSQL actualiza las FK que los referencian).
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipos_empleado')
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipo_empleados') THEN
    ALTER TABLE tipos_empleado RENAME TO tipo_empleados;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipos_vivienda')
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipo_viviendas') THEN
    ALTER TABLE tipos_vivienda RENAME TO tipo_viviendas;
  END IF;
END $$;

-- abogados.tarjeta_profesional (entidad: NOT NULL). Backfill con '' para
-- filas existentes y luego se quita el default.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'abogados' AND column_name = 'tarjeta_profesional') THEN
    ALTER TABLE abogados ADD COLUMN tarjeta_profesional VARCHAR(255) NOT NULL DEFAULT '';
    ALTER TABLE abogados ALTER COLUMN tarjeta_profesional DROP DEFAULT;
  END IF;
END $$;

-- casos: radicados e id_firma. Se añaden NULLABLE a propósito: el caso
-- raíz sentinel ('CASO MAESTRO SISTEMA') no tiene firma ni radicados, y
-- en una base nueva aún no existe ninguna firma cuando corre la migración.
-- El código (CasoServiceImpl) siempre asigna firma y radicados a los casos
-- reales; el sentinel es la única excepción documentada.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'casos' AND column_name = 'radicado_interno') THEN
    ALTER TABLE casos ADD COLUMN radicado_interno VARCHAR(10);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'casos' AND column_name = 'radicado_judicial') THEN
    ALTER TABLE casos ADD COLUMN radicado_judicial VARCHAR(23);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'casos' AND column_name = 'id_firma') THEN
    ALTER TABLE casos ADD COLUMN id_firma INTEGER REFERENCES firmas(id);
  END IF;
END $$;
-- Radicados del caso raíz sentinel (idempotente: solo si están en NULL).
UPDATE casos SET radicado_interno = '0000000000', radicado_judicial = '00000000000000000000000'
WHERE titulo = 'CASO MAESTRO SISTEMA' AND radicado_interno IS NULL;

-- agendas.id_abogado (la entidad y AgendaServiceImpl usan abogado, no
-- persona). Backfill desde abogados.id_persona cuando haya correspondencia.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'agendas' AND column_name = 'id_abogado') THEN
    ALTER TABLE agendas ADD COLUMN id_abogado INTEGER REFERENCES abogados(id);
  END IF;
END $$;
UPDATE agendas SET id_abogado = (SELECT id FROM abogados WHERE abogados.id_persona = agendas.id_persona LIMIT 1)
WHERE id_abogado IS NULL AND id_persona IS NOT NULL;
