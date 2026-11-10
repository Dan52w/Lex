-- ============================================================================
-- Flyway V1: esquema base rearmado (base de datos NUEVA).
-- Fuente: los 14 archivos SQL originales (001-015, sin 011) + LexCloud_P0_solo.sql
-- (ese va en V2). Todo verificado contra las entidades JPA del commit fdfb198.
--
-- CAMBIOS RESPECTO A LOS ARCHIVOS ORIGINALES (nada inventado, todo medido
-- contra el código; el detalle está en DB_REARMADO.md en la raíz del repo):
--   1. ORDEN: 014 (clientes) antes de 004, porque vinculo_casos tiene FK a
--      clientes(id). En el orden numérico original, 004 fallaba en base vacía.
--   2. NOMBRES: tipos_empleado -> tipo_empleados y tipos_vivienda ->
--      tipo_viviendas, como piden las entidades Tipo_Empleado/Tipo_Vivienda.
--   3. COLUMNAS QUE FALTABAN (las entidades las usan y el SQL no las tenía):
--      abogados.tarjeta_profesional, casos.radicado_interno,
--      casos.radicado_judicial, casos.id_firma, agendas.id_abogado.
--      agendas.id_persona NO se crea: ninguna entidad la usa (el 500 de
--      agenda venía de ahí: el código pedía id_abogado y la BD no la tenía).
--   4. niveles_salariales.nombre -> rango, como pide la entidad Nivel_Salarial.
--   5. LONGITUDES de catálogos alineadas a las entidades (ej. discapacidades
--      50 -> 100, estratos 20 -> 50). Los seeds caben en los nuevos largos.
--   6. equipo_abogados.activo y fecha_asignacion ahora NOT NULL (la entidad
--      los exige) y el UNIQUE(id_abogado, id_persona) pasa a índice único
--      parcial WHERE activo: así se puede reasignar a alguien tras darlo
--      de baja, cosa que el UNIQUE total impedía.
--   7. DEFAULT CURRENT_TIMESTAMP sobre columnas DATE pasa a CURRENT_DATE
--      (clientes.fecha_vinculacion, equipo_abogados.fecha_asignacion,
--      historial_cambios.fecha).
--   8. firmas.integrante ahora DEFAULT 0.
--   9. SEEDS DEL SISTEMA (antes no existían y los triggers los necesitan):
--      rol SISTEMA, firma SISTEMA, persona sistema cc 0000000000 y caso
--      maestro con id FIJO 1 (el código depende de id == 1: CasoServiceImpl
--      y AgendaServiceImpl lo usan como raíz). Tras el insert se ajusta la
--      secuencia con setval, para que el siguiente caso no choque con el 1.
--  10. Falta el archivo 011: no venía entre los compartidos y nada lo
--      referencia, así que no se inventó nada para cubrirlo.
--
-- En bases EXISTENTES se usa baseline-on-migrate=true: V1 se marca como
-- aplicada sin ejecutarse. Esta V1 es para base NUEVA (rearmado desde cero).
-- NUNCA editar una migración ya aplicada: los cambios van en V4, V5, ...
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 001_Raices.sql — catálogos raíz
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tipo_abogados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tipo_casos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS estados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS vinculos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tipo_documentos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tipo_agendas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- 002_Entidades.sql — personas y firmas
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS personas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    apellido VARCHAR(150) NOT NULL,
    cc VARCHAR(20) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_rol INTEGER NOT NULL REFERENCES roles(id),
    telefono VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_personas_activo ON personas(activo);
CREATE INDEX IF NOT EXISTS idx_personas_rol ON personas(id_rol);

CREATE TABLE IF NOT EXISTS firmas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    nit VARCHAR(50) NOT NULL UNIQUE,
    integrante INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_firmas_nombre ON firmas(nombre);

-- ----------------------------------------------------------------------------
-- 003_Estructura.sql — abogados, logins, subtipos y casos
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS abogados (
    id SERIAL PRIMARY KEY,
    id_persona INTEGER NOT NULL UNIQUE REFERENCES personas(id),
    id_firma INTEGER NOT NULL REFERENCES firmas(id),
    id_tipo_abogado INTEGER NOT NULL REFERENCES tipo_abogados(id),
    -- Corrección 3: la entidad Abogado exige tarjeta_profesional NOT NULL
    -- y el SQL original no la tenía.
    tarjeta_profesional VARCHAR(255) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_abogados_firma ON abogados(id_firma);
CREATE INDEX IF NOT EXISTS idx_abogados_tipo ON abogados(id_tipo_abogado);

CREATE TABLE IF NOT EXISTS logins (
    id_persona INTEGER PRIMARY KEY REFERENCES personas(id),
    correo VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS subtipo_casos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    id_tipo_caso INTEGER NOT NULL REFERENCES tipo_casos(id),
    UNIQUE(id_tipo_caso, nombre)
);
CREATE INDEX IF NOT EXISTS idx_subtipo_tipo ON subtipo_casos(id_tipo_caso);

CREATE TABLE IF NOT EXISTS casos (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT NOT NULL,
    id_estado INTEGER NOT NULL REFERENCES estados(id),
    id_subtipo_caso INTEGER NOT NULL REFERENCES subtipo_casos(id),
    fecha_inicio DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_caso_padre INTEGER REFERENCES casos(id),
    -- Corrección 3: la entidad Caso exige estas tres columnas NOT NULL
    -- (radicados UNIQUE) y el SQL original no las tenía.
    radicado_interno VARCHAR(10) NOT NULL UNIQUE,
    radicado_judicial VARCHAR(23) NOT NULL UNIQUE,
    id_firma INTEGER NOT NULL REFERENCES firmas(id)
);
CREATE INDEX IF NOT EXISTS idx_casos_busqueda ON casos(id_estado, id_subtipo_caso, activo);
CREATE INDEX IF NOT EXISTS idx_casos_padre ON casos(id_caso_padre);
CREATE INDEX IF NOT EXISTS idx_casos_firma ON casos(id_firma);

-- ----------------------------------------------------------------------------
-- 014_Table_Cliente.sql — catálogos de clientes y tabla clientes
-- (movido antes de 004 por la FK de vinculo_casos; ver encabezado, punto 1)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipo_clientes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO tipo_clientes (nombre) VALUES ('Natural'), ('Juridico')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS tipo_identificaciones (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO tipo_identificaciones (nombre) VALUES
('Cédula de Ciudadanía'), ('Cédula de Extranjería'), ('Pasaporte'), ('NIT')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS generos (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Genero pide length 50 (el original tenía 30).
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO generos (nombre) VALUES
('Masculino'), ('Femenino'), ('No Binario'), ('Prefiero no decirlo')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS estados_civiles (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Estado_Civil pide length 50 (el original tenía 30).
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO estados_civiles (nombre) VALUES
('Soltero(a)'), ('Casado(a)'), ('Unión Libre'), ('Divorciado(a)'), ('Viudo(a)')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS estratos_economicos (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Estrato_Economico pide length 50 (el original tenía 20).
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO estratos_economicos (nombre) VALUES
('Estrato 1'), ('Estrato 2'), ('Estrato 3'), ('Estrato 4'), ('Estrato 5'), ('Estrato 6')
ON CONFLICT (nombre) DO NOTHING;

-- Corrección 2: tipos_vivienda -> tipo_viviendas (entidad Tipo_Vivienda).
-- Corrección 5: length 100 (el original tenía 40).
CREATE TABLE IF NOT EXISTS tipo_viviendas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO tipo_viviendas (nombre) VALUES
('Propia'), ('Arrendada'), ('Familiar'), ('Compartida (Roomie)')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS niveles_salariales (
    id SERIAL PRIMARY KEY,
    -- Corrección 4: la entidad Nivel_Salarial llama a esta columna 'rango',
    -- no 'nombre'.
    rango VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO niveles_salariales (rango) VALUES
('Menos de 1 SMLV'), ('1 - 2 SMLV'), ('3 - 5 SMLV'), ('Más de 5 SMLV')
ON CONFLICT (rango) DO NOTHING;

-- Corrección 2: tipos_empleado -> tipo_empleados (entidad Tipo_Empleado).
-- Corrección 5: length 100 (el original tenía 50).
CREATE TABLE IF NOT EXISTS tipo_empleados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO tipo_empleados (nombre) VALUES
('Independiente'), ('Empleado Dependiente'), ('Desempleado'), ('Estudiante'), ('Pensionado')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS grados_escolaridad (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Grado_Escolaridad pide length 100 (el original tenía 60).
    nombre VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO grados_escolaridad (nombre) VALUES
('Primaria'), ('Secundaria / Bachillerato'), ('Técnico / Tecnólogo'), ('Universitario'), ('Postgrado')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS discapacidades (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Discapacidad pide length 100 (el original tenía 50).
    nombre VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO discapacidades (nombre) VALUES
('Ninguna'), ('Física / Motora'), ('Visual'), ('Auditiva'), ('Cognitiva / Mental'), ('Múltiple')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS clientes (
    id SERIAL PRIMARY KEY,
    id_firma INTEGER NOT NULL REFERENCES firmas(id),
    id_tipo_cliente INTEGER NOT NULL REFERENCES tipo_clientes(id),
    fecha_vinculacion DATE DEFAULT CURRENT_DATE,
    activo BOOLEAN DEFAULT TRUE,
    identificacion VARCHAR(50) NOT NULL,
    id_tipo_identificacion INTEGER REFERENCES tipo_identificaciones(id),
    nombres VARCHAR(100) NOT NULL,
    primer_apellido VARCHAR(100),
    segundo_apellido VARCHAR(100),
    id_genero INTEGER REFERENCES generos(id),
    id_estado_civil INTEGER REFERENCES estados_civiles(id),
    fecha_nacimiento DATE,
    lugar_nacimiento VARCHAR(150),
    nacionalidad VARCHAR(100),
    ciudad_residencia VARCHAR(150),
    localidad_sector VARCHAR(150),
    barrio VARCHAR(150),
    direccion_residencia VARCHAR(250),
    telefono_residencia VARCHAR(20),
    direccion_oficina VARCHAR(250),
    telefono_oficina VARCHAR(20),
    telefono_movil VARCHAR(20),
    correo VARCHAR(250) NOT NULL,
    id_estrato INTEGER REFERENCES estratos_economicos(id),
    id_vivienda INTEGER REFERENCES tipo_viviendas(id),
    id_nivel_salarial INTEGER REFERENCES niveles_salariales(id),
    id_tipo_empleado INTEGER REFERENCES tipo_empleados(id),
    id_grado_escolaridad INTEGER REFERENCES grados_escolaridad(id),
    id_discapacidad INTEGER REFERENCES discapacidades(id),
    CONSTRAINT clientes_identificacion_id_firma_key UNIQUE(identificacion, id_firma)
);
CREATE INDEX IF NOT EXISTS idx_clientes_firma ON clientes(id_firma);
CREATE INDEX IF NOT EXISTS idx_clientes_tipo ON clientes(id_tipo_cliente);
CREATE INDEX IF NOT EXISTS idx_clientes_identificacion ON clientes(identificacion);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo);

-- ----------------------------------------------------------------------------
-- 015_Table_Equipo.sql — equipo de abogados
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipo_relaciones_equipo (
    id SERIAL PRIMARY KEY,
    -- Corrección 5: la entidad Tipo_Relacion_Equipo pide length 100 (el original tenía 50).
    nombre VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO tipo_relaciones_equipo (nombre) VALUES
('Secretario(a)'),
('Asistente Legal'),
('Aprendiz / Judicante'),
('Co-Abogado Junior')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS equipo_abogados (
    id SERIAL PRIMARY KEY,
    id_abogado INTEGER NOT NULL REFERENCES abogados(id) ON DELETE CASCADE,
    id_persona INTEGER NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    id_tipo_relacion INTEGER NOT NULL REFERENCES tipo_relaciones_equipo(id),
    -- Corrección 6: la entidad exige fecha_asignacion y activo NOT NULL.
    fecha_asignacion DATE NOT NULL DEFAULT CURRENT_DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
    -- Sin UNIQUE(id_abogado, id_persona) a nivel de tabla: lo reemplaza el
    -- índice único parcial de abajo (permite reasignar tras una baja).
);
CREATE INDEX IF NOT EXISTS idx_equipo_abogado ON equipo_abogados(id_abogado);
CREATE INDEX IF NOT EXISTS idx_equipo_persona ON equipo_abogados(id_persona);
CREATE UNIQUE INDEX IF NOT EXISTS uq_equipo_abogado_persona_activo
    ON equipo_abogados(id_abogado, id_persona) WHERE activo = TRUE;

-- ----------------------------------------------------------------------------
-- 004_Seguimiento.sql — vínculos, agendas, documentos e historial
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinculo_casos (
    id SERIAL PRIMARY KEY,
    id_persona_firma INTEGER NOT NULL REFERENCES personas(id),
    id_cliente INTEGER NOT NULL REFERENCES clientes(id),
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    id_vinculo INTEGER NOT NULL REFERENCES vinculos(id),
    autorizado BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(id_persona_firma, id_cliente, id_caso)
);
CREATE INDEX IF NOT EXISTS idx_vinculo_caso ON vinculo_casos(id_caso);
CREATE INDEX IF NOT EXISTS idx_vinculo_persona ON vinculo_casos(id_persona_firma);
CREATE INDEX IF NOT EXISTS idx_vinculo_cliente ON vinculo_casos(id_cliente);

CREATE TABLE IF NOT EXISTS agendas (
    id SERIAL PRIMARY KEY,
    -- Corrección 3: la entidad Agenda usa id_abogado (FK a abogados), no
    -- id_persona. La columna vieja id_persona no se crea.
    id_abogado INTEGER NOT NULL REFERENCES abogados(id),
    id_tipo_agenda INTEGER NOT NULL REFERENCES tipo_agendas(id),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    descripcion TEXT NOT NULL,
    fecha_exp TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agendas_abogado ON agendas(id_abogado);
CREATE INDEX IF NOT EXISTS idx_agendas_caso ON agendas(id_caso);
CREATE INDEX IF NOT EXISTS idx_agendas_tipo ON agendas(id_tipo_agenda);

CREATE TABLE IF NOT EXISTS documentos (
    id SERIAL PRIMARY KEY,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    nombre VARCHAR(255) NOT NULL,
    direccion TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    tamanio BIGINT NOT NULL DEFAULT 0,
    id_tipo_documento INTEGER NOT NULL REFERENCES tipo_documentos(id)
);
CREATE INDEX IF NOT EXISTS idx_documentos_caso ON documentos(id_caso);
CREATE INDEX IF NOT EXISTS idx_documentos_tipo ON documentos(id_tipo_documento);

CREATE TABLE IF NOT EXISTS historial_cambios (
    id SERIAL PRIMARY KEY,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    descripcion TEXT NOT NULL,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    id_persona INTEGER NOT NULL REFERENCES personas(id)
);
CREATE INDEX IF NOT EXISTS idx_historial_caso ON historial_cambios(id_caso);
CREATE INDEX IF NOT EXISTS idx_historial_persona ON historial_cambios(id_persona);

-- ----------------------------------------------------------------------------
-- SEEDS DEL SISTEMA (corrección 9)
-- Los triggers de V3 (borrado de casos, protección del raíz) y el código
-- (id == 1) dependen de estos registros. Antes no estaban sembrados:
-- el trigger soft_delete_casos fallaba al borrar un caso porque buscaba
-- la persona cc 0000000000 y no existía (id_persona NULL -> violación
-- de NOT NULL en historial_cambios).
-- La persona sistema NO tiene login: nunca inicia sesión, solo firma la
-- auditoría interna de los triggers.
-- ----------------------------------------------------------------------------
INSERT INTO roles (nombre) VALUES ('SISTEMA')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO firmas (nombre, nit, integrante) VALUES ('FIRMA SISTEMA', '0000000000', 0)
ON CONFLICT (nit) DO NOTHING;

INSERT INTO personas (nombre, apellido, cc, activo, id_rol, telefono, fecha_nacimiento)
VALUES ('SISTEMA', 'SISTEMA', '0000000000', TRUE,
        (SELECT id FROM roles WHERE nombre = 'SISTEMA'),
        '0000000000', DATE '1900-01-01')
ON CONFLICT (cc) DO NOTHING;

-- 005_Record_Zero.sql — catálogos base del caso raíz
INSERT INTO estados (nombre) VALUES ('SIN DEFINIR')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO tipo_casos (nombre) VALUES ('RAÍZ')
ON CONFLICT (nombre) DO NOTHING;

-- 'Principal' es el vínculo que el código asigna al crear un caso
-- (CasoServiceImpl.crearVinculo lo busca por nombre, no por id).
INSERT INTO vinculos (nombre) VALUES ('SIN VÍNCULO'), ('Principal')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO subtipo_casos (nombre, id_tipo_caso)
VALUES ('RAÍZ', (SELECT id FROM tipo_casos WHERE nombre = 'RAÍZ'))
ON CONFLICT (id_tipo_caso, nombre) DO NOTHING;

-- El caso maestro con id FIJO 1: CasoServiceImpl (líneas 65, 103, 241, 251)
-- y AgendaServiceImpl comparan id == 1 para identificar la raíz.
-- Radicados en ceros: el DTO exige 23 dígitos para el judicial y 10
-- para el interno; el maestro no es un caso real, así que van en ceros.
-- id_firma apunta a la FIRMA SISTEMA (la entidad exige id_firma NOT NULL).
INSERT INTO casos (
    id, titulo, descripcion, id_estado, id_subtipo_caso,
    fecha_inicio, activo, id_caso_padre,
    radicado_interno, radicado_judicial, id_firma
) VALUES (
    1,
    'CASO MAESTRO SISTEMA',
    'Registro raíz para jerarquía de casos. No eliminar.',
    (SELECT id FROM estados WHERE nombre = 'SIN DEFINIR'),
    (SELECT id FROM subtipo_casos WHERE nombre = 'RAÍZ'),
    CURRENT_DATE,
    FALSE,
    NULL,
    '0000000000',
    '00000000000000000000000',
    (SELECT id FROM firmas WHERE nit = '0000000000')
)
ON CONFLICT (id) DO NOTHING;

-- El insert con id explícito no avanza la secuencia: sin este setval, el
-- próximo caso intentaría usar el id 1 y fallaría por clave duplicada.
SELECT setval('casos_id_seq', (SELECT MAX(id) FROM casos));
