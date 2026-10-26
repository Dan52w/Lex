-- Flyway V1: línea base. Representa el esquema tal como está en la base
-- de datos actual del usuario (los 14 archivos SQL consolidados, con
-- clientes antes de vinculo_casos y con los nombres tipo_empleados /
-- tipo_viviendas ya corregidos en el script consolidado).
-- En bases EXISTENTES se usa baseline-on-migrate=true: V1 se marca como
-- aplicada sin ejecutarse, y solo corren V2 en adelante. En bases NUEVAS
-- V1 se ejecuta completa y luego V2, V3 y V4.
-- NUNCA editar una migración ya aplicada: los cambios van en migraciones
-- nuevas (V5, V6, ...).

-- ============================================================================
-- LexCloud — Base de datos completa en un solo script (PostgreSQL)
-- ============================================================================
-- Contenido: los 14 archivos SQL que compartiste, consolidados, + las 2
-- tablas nuevas del P0 que faltaban y causaban tu error de login.
--
-- CÓMO USARLO (importante):
--   * Es para una base de datos VACÍA, y se ejecuta UNA sola vez.
--   * Si tu db_Lex actual ya tiene datos, NO ejecutes este archivo completo:
--     ejecuta SOLO la sección final "P0" (refresh_tokens y
--     auditoria_seguridad), que es lo que te falta para iniciar sesión.
--   * Con el application.properties corregido (ddl-auto=update) el backend
--     crea/ajusta el resto al arrancar; con ddl-auto=none este script es
--     obligatorio antes de arrancar.
--
-- CAMBIOS RESPECTO A LOS ARCHIVOS ORIGINALES (no se inventó ningún dato):
--   1. ORDEN: 014 (clientes) se movió antes de 004, porque vinculo_casos
--      tiene FK a clientes(id). En el orden numérico original, 004 fallaba
--      en una base vacía: clientes aún no existía.
--   2. NOMBRES: tipos_empleado -> tipo_empleados y
--      tipos_vivienda -> tipo_viviendas, para que coincidan con las
--      entidades Java (Tipo_Empleado y Tipo_Vivienda). Con los nombres
--      viejos, el backend buscaba tablas que no existían.
--   3. CREATE TABLE -> CREATE TABLE IF NOT EXISTS (el resto, verbatim).
--   4. Se añadió la sección P0 al final (DDL exacto de README-P0.md).
--   5. Falta el archivo 011: no venía entre los compartidos y ningún otro
--      archivo lo referencia, así que no se inventó nada para cubrirlo.
--
-- AVISO CONOCIDO (viene de los archivos originales, no es nuevo):
--   El trigger soft_delete_casos inserta en historial_cambios con la
--   persona de cc '0000000000' (persona sistema). Esa persona NO está
--   sembrada en estos archivos: si borras un caso antes de crearla, ese
--   trigger fallará. Créala por el backend (primer admin) antes de usar
--   borrados de casos.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- Archivo original: 001_Raices.sql
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
-- Archivo original: 002_Entidades.sql
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
CREATE INDEX idx_personas_activo ON personas(activo);
CREATE INDEX idx_personas_rol ON personas(id_rol);

CREATE TABLE IF NOT EXISTS firmas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    nit VARCHAR(50) NOT NULL UNIQUE,
    integrante INTEGER NOT NULL 
);
CREATE INDEX idx_firmas_nombre ON firmas(nombre);

-- ----------------------------------------------------------------------------
-- Archivo original: 003_Estructura.sql
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS abogados (
    id SERIAL PRIMARY KEY,
    id_persona INTEGER NOT NULL UNIQUE REFERENCES personas(id),
    id_firma INTEGER NOT NULL REFERENCES firmas(id),
    id_tipo_abogado INTEGER NOT NULL REFERENCES tipo_abogados(id)
);

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

CREATE TABLE IF NOT EXISTS casos (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT NOT NULL,
    id_estado INTEGER NOT NULL REFERENCES estados(id),
    id_subtipo_caso INTEGER NOT NULL REFERENCES subtipo_casos(id),
    fecha_inicio DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_caso_padre INTEGER REFERENCES casos(id) -- Auto-referencia
);
CREATE INDEX idx_casos_busqueda ON casos(id_estado, id_subtipo_caso, activo);

-- ----------------------------------------------------------------------------
-- Archivo original: 014_Table_Cliente.sql
-- ----------------------------------------------------------------------------
-- 1. Crear tablas de referencia y catálogos primero
CREATE TABLE IF NOT EXISTS tipo_clientes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO tipo_clientes (nombre) VALUES ('Natural'), ('Juridico');

CREATE TABLE IF NOT EXISTS tipo_identificaciones (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO tipo_identificaciones (nombre) VALUES 
('Cédula de Ciudadanía'), ('Cédula de Extranjería'), ('Pasaporte'), ('NIT');

CREATE TABLE IF NOT EXISTS generos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO generos (nombre) VALUES 
('Masculino'), ('Femenino'), ('No Binario'), ('Prefiero no decirlo');

CREATE TABLE IF NOT EXISTS estados_civiles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO estados_civiles (nombre) VALUES 
('Soltero(a)'), ('Casado(a)'), ('Unión Libre'), ('Divorciado(a)'), ('Viudo(a)');

CREATE TABLE IF NOT EXISTS estratos_economicos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO estratos_economicos (nombre) VALUES 
('Estrato 1'), ('Estrato 2'), ('Estrato 3'), ('Estrato 4'), ('Estrato 5'), ('Estrato 6');

CREATE TABLE IF NOT EXISTS tipo_viviendas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE
);
INSERT INTO tipo_viviendas (nombre) VALUES 
('Propia'), ('Arrendada'), ('Familiar'), ('Compartida (Roomie)');

CREATE TABLE IF NOT EXISTS niveles_salariales (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO niveles_salariales (nombre) VALUES 
('Menos de 1 SMLV'), ('1 - 2 SMLV'), ('3 - 5 SMLV'), ('Más de 5 SMLV');

CREATE TABLE IF NOT EXISTS tipo_empleados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO tipo_empleados (nombre) VALUES 
('Independiente'), ('Empleado Dependiente'), ('Desempleado'), ('Estudiante'), ('Pensionado');

CREATE TABLE IF NOT EXISTS grados_escolaridad (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE
);
INSERT INTO grados_escolaridad (nombre) VALUES 
('Primaria'), ('Secundaria / Bachillerato'), ('Técnico / Tecnólogo'), ('Universitario'), ('Postgrado');

CREATE TABLE IF NOT EXISTS discapacidades (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO discapacidades (nombre) VALUES 
('Ninguna'), ('Física / Motora'), ('Visual'), ('Auditiva'), ('Cognitiva / Mental'), ('Múltiple');


-- 2. Crear la tabla clientes completamente independiente de "personas"
CREATE TABLE IF NOT EXISTS clientes (
    id SERIAL PRIMARY KEY,
    id_firma INTEGER NOT NULL REFERENCES firmas(id),
    id_tipo_cliente INTEGER NOT NULL REFERENCES tipo_clientes(id),
    fecha_vinculacion DATE DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    
    -- Sección: Datos del Tercero
    identificacion VARCHAR(50) NOT NULL,
    id_tipo_identificacion INTEGER REFERENCES tipo_identificaciones(id),
    nombres VARCHAR(100) NOT NULL,
    primer_apellido VARCHAR(100),
    segundo_apellido VARCHAR(100),
    id_genero INTEGER REFERENCES generos(id),
    
    -- Sección: Información Adicional
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
    correo VARCHAR(250) NOT NULL, -- Corresponde al campo "E-mail" del formulario
    id_estrato INTEGER REFERENCES estratos_economicos(id),
    id_vivienda INTEGER REFERENCES tipo_viviendas(id),
    id_nivel_salarial INTEGER REFERENCES niveles_salariales(id),
    id_tipo_empleado INTEGER REFERENCES tipo_empleados(id),
    id_grado_escolaridad INTEGER REFERENCES grados_escolaridad(id),
    id_discapacidad INTEGER REFERENCES discapacidades(id),

    -- Restricción para asegurar unicidad del cliente dentro de una misma firma
    CONSTRAINT clientes_identificacion_id_firma_key UNIQUE(identificacion, id_firma)
);


-- 3. Índices para optimizar las consultas del ecosistema
CREATE INDEX idx_clientes_firma ON clientes(id_firma);
CREATE INDEX idx_clientes_tipo ON clientes(id_tipo_cliente);
CREATE INDEX idx_clientes_identificacion ON clientes(identificacion);

-- ----------------------------------------------------------------------------
-- Archivo original: 015_Table_Equipo.sql
-- ----------------------------------------------------------------------------
-- 1. Tabla de catálogo para definir el tipo de relación o rol en el equipo
CREATE TABLE IF NOT EXISTS tipo_relaciones_equipo (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Insertamos los roles comunes según tu ejemplo
INSERT INTO tipo_relaciones_equipo (nombre) VALUES 
('Secretario(a)'), 
('Asistente Legal'), 
('Aprendiz / Judicante'),
('Co-Abogado Junior');

-- 2. Tabla intermedia para armar el equipo del abogado
CREATE TABLE IF NOT EXISTS equipo_abogados (
    id SERIAL PRIMARY KEY,
    id_abogado INTEGER NOT NULL REFERENCES abogados(id) ON DELETE CASCADE, -- El Abogado "Jefe"
    id_persona INTEGER NOT NULL REFERENCES personas(id) ON DELETE CASCADE, -- El subordinado (Secretaria, Asistente, etc.)
    id_tipo_relacion INTEGER NOT NULL REFERENCES tipo_relaciones_equipo(id), -- El rol que cumple
    fecha_asignacion DATE DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    
    -- Restricción para evitar duplicar exactamente la misma asignación activa
    CONSTRAINT unique_equipo_abogado_persona UNIQUE (id_abogado, id_persona)
);

-- Índices para optimizar las consultas de rendimiento del equipo
CREATE INDEX idx_equipo_abogado ON equipo_abogados(id_abogado);
CREATE INDEX idx_equipo_persona ON equipo_abogados(id_persona);

-- ----------------------------------------------------------------------------
-- Archivo original: 004_Seguimiento.sql
-- ----------------------------------------------------------------------------
-- 1. Tabla Vinculo Casos (id_cliente corregido para apuntar a 'clientes')
CREATE TABLE IF NOT EXISTS vinculo_casos (
    id SERIAL PRIMARY KEY,
    id_persona_firma INTEGER NOT NULL REFERENCES personas(id),
    id_cliente INTEGER NOT NULL REFERENCES clientes(id),
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    id_vinculo INTEGER NOT NULL REFERENCES vinculos(id),
    autorizado BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(id_persona_firma, id_cliente, id_caso)
);

-- 2. Tabla Agendas (id_abogado renombrado a id_persona y referenciando a 'personas')
CREATE TABLE IF NOT EXISTS agendas (
    id SERIAL PRIMARY KEY,
    id_persona INTEGER NOT NULL REFERENCES personas(id),
    id_tipo_agenda INTEGER NOT NULL REFERENCES tipo_agendas(id),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    descripcion TEXT NOT NULL,
    fecha_exp TIMESTAMP NOT NULL
);

-- 3. Tabla Documentos (Sin cambios)
CREATE TABLE IF NOT EXISTS documentos (
    id SERIAL PRIMARY KEY,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    nombre VARCHAR(255) NOT NULL,
    direccion TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    tamanio BIGINT NOT NULL DEFAULT 0,
    id_tipo_documento INTEGER NOT NULL REFERENCES tipo_documentos(id)
);

-- 4. Tabla Historial Cambios (Sin cambios)
CREATE TABLE IF NOT EXISTS historial_cambios (
    id SERIAL PRIMARY KEY,
    fecha DATE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT NOT NULL,
    id_caso INTEGER NOT NULL REFERENCES casos(id),
    id_persona INTEGER NOT NULL REFERENCES personas(id)
);

-- ----------------------------------------------------------------------------
-- Archivo original: 005_Record_Zero.sql
-- ----------------------------------------------------------------------------
-- ETAPA 1: Catálogos Maestros
-- Insertamos el estado inicial y el tipo de caso base
INSERT INTO estados (nombre) 
VALUES ('SIN DEFINIR');

INSERT INTO tipo_casos (nombre) 
VALUES ('RAÍZ');

INSERT INTO vinculos (nombre) 
VALUES ('SIN VÍNCULO');

-- ETAPA 2: Subtipos
-- Necesitamos un subtipo que apunte al tipo de caso base
INSERT INTO subtipo_casos (nombre, id_tipo_caso) 
VALUES ('RAÍZ', (SELECT id FROM tipo_casos WHERE nombre = 'RAÍZ'));

-- ETAPA 3: El Caso Padre (Sentinel Record)
-- Insertamos el registro con ID manual o capturando el ID generado
-- Este será el id_caso_padre de todos los futuros casos de primer nivel.
INSERT INTO casos (
    titulo, 
    descripcion, 
    id_estado, 
    id_subtipo_caso, 
    fecha_inicio, 
    activo, 
    id_caso_padre
) VALUES (
    'CASO MAESTRO SISTEMA',
    'Registro raíz para jerarquía de casos. No eliminar.',
    (SELECT id FROM estados WHERE nombre = 'SIN DEFINIR'),
    (SELECT id FROM subtipo_casos WHERE nombre = 'RAÍZ'),
    CURRENT_DATE,
    FALSE,
    NULL -- El único que puede ser NULL es el origen absoluto
);

-- ----------------------------------------------------------------------------
-- Archivo original: 006_root_case.sql
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_root_case()
RETURNS trigger AS $$
BEGIN
    -- Ajustamos nombres de columnas: id_caso_padre y titulo
    IF NEW.id_caso_padre IS NULL 
       AND NEW.titulo <> 'CASO MAESTRO SISTEMA' THEN

        SELECT id INTO NEW.id_caso_padre
        FROM casos
        WHERE titulo = 'CASO MAESTRO SISTEMA'
        LIMIT 1;

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_root_case
BEFORE INSERT ON casos
FOR EACH ROW
EXECUTE FUNCTION set_root_case();

-- ----------------------------------------------------------------------------
-- Archivo original: 007_delete_records_sistema.sql
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_delete_records_sistema()
RETURNS trigger AS $$
BEGIN
    -- Si es en la tabla personas, protegemos la CC especial
    IF TG_TABLE_NAME = 'personas' AND OLD.cc = '0000000000' THEN
        RAISE EXCEPTION 'Operación denegada: No se puede eliminar el registro base del sistema.';
    END IF;

    -- Si es en la tabla casos, protegemos el título raíz
    IF TG_TABLE_NAME = 'casos' AND OLD.titulo = 'CASO MAESTRO SISTEMA' THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Maestro es vital para la jerarquía.';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Aplicamos a Personas
CREATE TRIGGER trg_no_delete_persona_sistema
BEFORE DELETE ON personas
FOR EACH ROW EXECUTE FUNCTION prevent_delete_records_sistema();

-- Aplicamos a Casos (Seguridad extra)
CREATE TRIGGER trg_no_delete_caso_raiz
BEFORE DELETE ON casos
FOR EACH ROW EXECUTE FUNCTION prevent_delete_records_sistema();

-- ----------------------------------------------------------------------------
-- Archivo original: 008_subtipo_exists.sql
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION validate_subtipo_exists()
RETURNS trigger AS $$
DECLARE
    tipo_id_encontrado integer;
BEGIN
    -- Verificamos si el subtipo existe y obtenemos su tipo padre
    SELECT id_tipo_caso INTO tipo_id_encontrado
    FROM subtipo_casos
    WHERE id = NEW.id_subtipo_caso;

    IF tipo_id_encontrado IS NULL THEN
        RAISE EXCEPTION 'Error: El id_subtipo_caso (%) no existe en la configuración.', NEW.id_subtipo_caso;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_subtipo
BEFORE INSERT OR UPDATE ON casos
FOR EACH ROW
EXECUTE FUNCTION validate_subtipo_exists();

-- ----------------------------------------------------------------------------
-- Archivo original: 009_default_estado.sql
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_default_estado()
RETURNS trigger AS $$
BEGIN
    IF NEW.id_estado IS NULL THEN
        SELECT id INTO NEW.id_estado
        FROM estados
        WHERE nombre = 'SIN DEFINIR'
        LIMIT 1;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_default_estado
BEFORE INSERT ON casos
FOR EACH ROW
EXECUTE FUNCTION set_default_estado();

-- ----------------------------------------------------------------------------
-- Archivo original: 010_soft_delete_personas.sql
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION soft_delete_personas()
RETURNS TRIGGER AS $$
BEGIN
    -- 1. Verificación de seguridad: No permitir borrar (ni lógica ni físicamente) al usuario sistema
    IF OLD.cc = '0000000000' THEN
        RAISE EXCEPTION 'Operación denegada: El registro raíz del sistema no puede ser alterado.';
    END IF;

    -- 2. Ejecutar el borrado lógico
    UPDATE personas
    SET activo = FALSE
    WHERE id = OLD.id;

    -- 3. Mensaje informativo (opcional, útil para debugging)
    RAISE NOTICE 'Aviso: El registro con ID % ha sido desactivado (Soft Delete).', OLD.id;

    -- 4. CRUCIAL: Retornar NULL para cancelar la operación DELETE física
    RETURN NULL; 
END;
$$ LANGUAGE plpgsql;

-- Creamos el trigger para la tabla personas
CREATE TRIGGER trg_soft_delete_personas
BEFORE DELETE ON personas
FOR EACH ROW
EXECUTE FUNCTION soft_delete_personas();

-- ----------------------------------------------------------------------------
-- Archivo original: 012_prevent_delete_root_case.sql
-- ----------------------------------------------------------------------------
-- 1. Función para prevenir el BORRADO del caso raíz
CREATE OR REPLACE FUNCTION prevent_delete_root_case()
RETURNS TRIGGER AS $$
BEGIN
    -- En nuestro esquema, el raíz es el único cuyo id_caso_padre ES NULL
    IF OLD.id_caso_padre IS NULL THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz es un registro del sistema y no puede ser eliminado.';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_delete_root_case
BEFORE DELETE ON casos
FOR EACH ROW
EXECUTE FUNCTION prevent_delete_root_case();

-- 2. Función para prevenir la MODIFICACIÓN del caso raíz
CREATE OR REPLACE FUNCTION prevent_update_caso_raiz()
RETURNS TRIGGER AS $$
BEGIN
    -- Protegemos el registro que no tiene padre (el original)
    IF OLD.id_caso_padre IS NULL THEN
        -- Opcional: Permitir cambios menores pero no de columnas críticas
        -- Aquí bloqueamos cualquier actualización para máxima seguridad
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz del sistema es de solo lectura para integridad de la jerarquía.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_caso_raiz
BEFORE UPDATE ON casos
FOR EACH ROW
EXECUTE FUNCTION prevent_update_caso_raiz();

-- ----------------------------------------------------------------------------
-- Archivo original: 013_soft_delete_casos.sql
-- ----------------------------------------------------------------------------


CREATE OR REPLACE FUNCTION soft_delete_casos()
RETURNS TRIGGER AS $$
BEGIN
    -- 1. Protección de Seguridad: El Caso Raíz no se puede desactivar
    IF OLD.id_caso_padre IS NULL THEN
        RAISE EXCEPTION 'Operación denegada: El Caso Raíz del sistema debe permanecer siempre activo.';
    END IF;

    -- 2. Ejecutar el borrado lógico
    -- Eliminamos "sentiments" porque no existe en tu tabla original
    UPDATE casos
    SET activo = FALSE
    WHERE id = OLD.id;

    -- 3. Registro en auditoría
    INSERT INTO historial_cambios (
        id_caso, 
        id_persona, 
        fecha, 
        descripcion
    )
    VALUES (
        OLD.id, 
        (SELECT id FROM personas WHERE cc = '0000000000' LIMIT 1), 
        CURRENT_TIMESTAMP, 
        'BORRADO LÓGICO: El caso fue marcado como inactivo.'
    );

    -- 4. Cancelar el DELETE físico
    RETURN NULL; 
END;
$$ LANGUAGE plpgsql;

-- Ejecuta esto solo si no habías creado el trigger antes
DROP TRIGGER IF EXISTS trg_soft_delete_casos ON casos;

CREATE TRIGGER trg_soft_delete_casos
BEFORE DELETE ON casos
FOR EACH ROW
EXECUTE FUNCTION soft_delete_casos();


