-- ============================================================================
-- LexCloud — Datos semilla de prueba (datos_semilla.sql)
-- ============================================================================
-- DATOS FICTICIOS SOLO PARA PRUEBAS. Nunca datos reales.
-- Todas las personas, firmas, clientes, casos y correos son inventados
-- (dominio @lexdemo.local, cédulas/nits de prueba). No usar en producción
-- ni mezclar con datos reales de clientes.
--
-- Contraseña de prueba de TODAS las cuentas: ver la guía
-- datos-semilla-2026-11-12.md §5 (SOLO para pruebas; cambiarla antes de
-- cualquier uso real). No se escribe aquí en claro a propósito.
--
-- Uso: ejecutar en pgAdmin DESPUÉS de aplicar las migraciones V1–V6.
-- NO es una migración Flyway: vive en db/seed/, fuera de db/migrations/,
-- así que Flyway no lo ve (spring.flyway.locations=classpath:db/migrations).
-- Nunca renombrarlo como V7__... ni moverlo a db/migrations/.
--
-- Idempotente y re-ejecutable: correrlo dos veces no duplica nada
-- (ON CONFLICT DO NOTHING / guardas NOT EXISTS contra las constraints
-- reales). Los ids se resuelven por búsqueda (cc, nit, correo, título),
-- nunca con ids fijos.
--
-- Todo corre en UNA sola transacción: si algo falla, no queda nada parcial.
-- Diseño seguido: seed-diseno.md (Fase 1, 2026-11-12).
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 0. PRE-VUELO — aborta antes de insertar nada si la base no está lista
--    o si algún dato del seed choca con un registro REAL distinto.
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_bad text;
BEGIN
    -- Base del sistema (V1–V6 aplicadas)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE id = 1 AND titulo = 'CASO MAESTRO SISTEMA') THEN
        RAISE EXCEPTION 'Pre-vuelo: no existe el caso maestro (id = 1, CASO MAESTRO SISTEMA). Aplica V1 antes del seed.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM personas WHERE cc = '0000000000') THEN
        RAISE EXCEPTION 'Pre-vuelo: no existe la persona sistema (cc 0000000000). Aplica V1 antes del seed.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'public' AND sequencename = 'radicado_seq') THEN
        RAISE EXCEPTION 'Pre-vuelo: no existe la secuencia radicado_seq. Aplica V4 antes del seed.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ADMINISTRADOR') THEN
        RAISE EXCEPTION 'Pre-vuelo: no existe el rol ADMINISTRADOR (V4). Aplica V4 antes del seed.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'SUPER_ADMIN') THEN
        RAISE EXCEPTION 'Pre-vuelo: no existe el rol SUPER_ADMIN (V5). Aplica V5 antes del seed.';
    END IF;

    -- Colisión de cédulas: una cc del seed con otros datos = dato real.
    -- Se comprueba la terna (cc, nombre, apellido): una cc con el nombre
    -- de OTRA persona del seed también es colisión (p. ej. cc cruzadas).
    SELECT string_agg(cc, ', ') INTO v_bad FROM personas
    WHERE cc IN ('1000000001','1000000002','1000000003','1000000004','1000000005',
                 '1000000006','1000000007','1000000008','1000000009','1000000010','1000000011')
      AND (cc, nombre, apellido) NOT IN (
            ('1000000001','Sofía','Mendoza Ruiz'), ('1000000002','Carlos','Rojas Medina'),
            ('1000000003','Diana','Páez Cárdenas'), ('1000000004','Felipe','Navarro Ortiz'),
            ('1000000005','Valentina','Castro Gil'), ('1000000006','Mateo','Silva Torres'),
            ('1000000007','Camilo','Duarte Peña'), ('1000000008','Isabella','Ramos Soto'),
            ('1000000009','Sebastián','Vega Luna'), ('1000000010','Laura','Gómez Niño'),
            ('1000000011','Natalia','Ortiz Rey'));
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Pre-vuelo: las cc % ya existen con OTROS datos (posible dato real). Cambia el rango del seed antes de ejecutar; no continúes.', v_bad;
    END IF;

    -- Colisión de NIT de firmas demo (se comprueba el par nit+nombre:
    -- un nit con el nombre de la OTRA firma demo también es colisión)
    SELECT string_agg(nit, ', ') INTO v_bad FROM firmas
    WHERE nit IN ('9000000011','9000000022')
      AND (nit, nombre) NOT IN (('9000000011','Firma Demo Norte'),
                                ('9000000022','Firma Demo Sur'));
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Pre-vuelo: los NIT % ya existen con OTRO nombre de firma. Cambia los NIT del seed antes de ejecutar.', v_bad;
    END IF;

    -- Colisión de correos: un correo del seed que pertenezca a otra persona.
    -- Se comprueba el par (correo, cc): un correo del seed en la persona
    -- equivocada del seed también es colisión (no basta con que la persona
    -- sea del seed; el WHERE correo IN ... evita tocar correos reales).
    SELECT string_agg(l.correo, ', ') INTO v_bad FROM logins l
    JOIN personas p ON p.id = l.id_persona
    WHERE l.correo IN ('superadmin@lexdemo.local','admin.norte@lexdemo.local',
                     'admin.sur@lexdemo.local','felipe.navarro@lexdemo.local',
                     'valentina.castro@lexdemo.local','mateo.silva@lexdemo.local',
                     'camilo.duarte@lexdemo.local','isabella.ramos@lexdemo.local',
                     'sebastian.vega@lexdemo.local','laura.gomez@lexdemo.local',
                     'natalia.ortiz@lexdemo.local')
      AND (l.correo, p.cc) NOT IN (
            ('superadmin@lexdemo.local','1000000001'),
            ('admin.norte@lexdemo.local','1000000002'),
            ('admin.sur@lexdemo.local','1000000003'),
            ('felipe.navarro@lexdemo.local','1000000004'),
            ('valentina.castro@lexdemo.local','1000000005'),
            ('mateo.silva@lexdemo.local','1000000006'),
            ('camilo.duarte@lexdemo.local','1000000007'),
            ('isabella.ramos@lexdemo.local','1000000008'),
            ('sebastian.vega@lexdemo.local','1000000009'),
            ('laura.gomez@lexdemo.local','1000000010'),
            ('natalia.ortiz@lexdemo.local','1000000011'));
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Pre-vuelo: los correos % ya pertenecen a OTRA persona. Cambia los correos del seed antes de ejecutar.', v_bad;
    END IF;

    RAISE NOTICE 'Pre-vuelo OK: caso maestro, persona sistema, radicado_seq y roles base existen; sin colisiones con datos reales.';
END $$;

-- ----------------------------------------------------------------------------
-- 1–11. SIEMBRA (un solo bloque DO con variables; ids siempre por búsqueda)
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    -- Roles
    v_rol_superadmin   int;
    v_rol_admin        int;
    v_rol_abogado      int;
    v_rol_secretaria   int;
    -- Catálogos de casos
    v_tc_civil         int;
    v_tc_penal         int;
    v_tc_laboral       int;
    v_tc_familia       int;
    v_tc_comercial     int;
    v_tc_admin         int;
    v_est_sin_definir  int;
    v_est_tramite      int;
    v_est_espera       int;
    v_est_terminado    int;
    v_est_cerrado      int;
    v_st_resp_civil    int;
    v_st_contratos     int;
    v_st_lesiones      int;
    v_st_despido       int;
    v_st_recl_salarial int;
    v_st_divorcio      int;
    v_st_custodia      int;
    v_st_cobro         int;
    v_st_tutela        int;
    v_ta_socio         int;
    v_ta_senior        int;
    v_ta_junior        int;
    v_tag_audiencia    int;
    v_tag_consulta     int;
    v_tag_reunion      int;
    v_tag_seguimiento  int;
    v_tag_diligencia   int;
    v_vinc_principal   int;
    v_vinc_coabogado   int;
    v_rel_secretario   int;
    -- Catálogos de clientes
    v_tipo_nat         int;
    v_tipo_jur         int;
    v_tid_cc           int;
    v_tid_nit          int;
    v_gen_m            int;
    v_gen_f            int;
    v_ec_soltero       int;
    v_ec_casado        int;
    v_est2             int;
    v_est3             int;
    v_est4             int;
    v_viv_propia       int;
    v_viv_arrendada    int;
    v_ns_1_2           int;
    v_ns_3_5           int;
    v_te_dep           int;
    v_te_ind           int;
    v_te_est           int;
    v_ge_uni           int;
    v_ge_tec           int;
    v_disc_ninguna     int;
    -- Firmas
    v_f_norte          int;
    v_f_sur            int;
    -- Personas (P01–P11)
    v_p01 int; v_p02 int; v_p03 int; v_p04 int; v_p05 int; v_p06 int;
    v_p07 int; v_p08 int; v_p09 int; v_p10 int; v_p11 int;
    -- Abogados
    v_ab_p04 int; v_ab_p05 int; v_ab_p06 int;
    v_ab_p07 int; v_ab_p08 int; v_ab_p09 int;
    -- Clientes
    v_c_cn1 int; v_c_cn2 int; v_c_cn3 int; v_c_cn4 int;
    v_c_cs1 int; v_c_cs2 int; v_c_cs3 int;
    -- Casos
    v_caso_n1 int; v_caso_n2 int; v_caso_n3 int; v_caso_n4 int; v_caso_n5 int;
    v_caso_s1 int; v_caso_s2 int; v_caso_s3 int; v_caso_s4 int; v_caso_s5 int;
    -- Hash BCrypt (coste 10) de la contraseña de prueba (ver guía §5),
    -- generado y verificado con BCryptPasswordEncoder de Spring Security.
    v_hash constant text := '$2a$10$FuHT4npxLaUXIH9nu4jO0OAAWcuqOLtaK5fvq4Cy9gGaXDeXJT.yO';
BEGIN
    -------------------------------------------------------------------------
    -- 1. Roles nuevos ABOGADO y SECRETARIA (es_admin = FALSE).
    --    No existen en V1–V6; son imprescindibles porque personas.id_rol
    --    es NOT NULL y el modelo de 3 niveles exige distinguir abogado y
    --    secretaria sin darles privilegios de admin.
    -------------------------------------------------------------------------
    INSERT INTO roles (nombre, es_admin) VALUES
        ('ABOGADO', FALSE),
        ('SECRETARIA', FALSE)
    ON CONFLICT (nombre) DO UPDATE SET es_admin = EXCLUDED.es_admin;

    -------------------------------------------------------------------------
    -- 2. Catálogos nuevos (propuestas del diseño §7.2; ver guía §11).
    -------------------------------------------------------------------------
    INSERT INTO estados (nombre) VALUES
        ('EN TRÁMITE'), ('EN ESPERA'), ('TERMINADO'), ('CERRADO')
    ON CONFLICT (nombre) DO NOTHING;

    INSERT INTO tipo_casos (nombre) VALUES
        ('Civil'), ('Penal'), ('Laboral'), ('Familia'), ('Comercial'), ('Administrativo')
    ON CONFLICT (nombre) DO NOTHING;

    SELECT id INTO v_tc_civil     FROM tipo_casos WHERE nombre = 'Civil';
    SELECT id INTO v_tc_penal     FROM tipo_casos WHERE nombre = 'Penal';
    SELECT id INTO v_tc_laboral   FROM tipo_casos WHERE nombre = 'Laboral';
    SELECT id INTO v_tc_familia   FROM tipo_casos WHERE nombre = 'Familia';
    SELECT id INTO v_tc_comercial FROM tipo_casos WHERE nombre = 'Comercial';
    SELECT id INTO v_tc_admin     FROM tipo_casos WHERE nombre = 'Administrativo';

    INSERT INTO subtipo_casos (id_tipo_caso, nombre) VALUES
        (v_tc_civil,     'Responsabilidad Civil'),
        (v_tc_civil,     'Contratos'),
        (v_tc_penal,     'Lesiones Personales'),
        (v_tc_laboral,   'Despido Injustificado'),
        (v_tc_laboral,   'Reclamación Salarial'),
        (v_tc_familia,   'Divorcio'),
        (v_tc_familia,   'Custodia'),
        (v_tc_comercial, 'Cobro de Cartera'),
        (v_tc_admin,     'Acción de Tutela')
    ON CONFLICT (id_tipo_caso, nombre) DO NOTHING;

    INSERT INTO tipo_abogados (nombre) VALUES
        ('Socio'), ('Asociado Senior'), ('Asociado Junior')
    ON CONFLICT (nombre) DO NOTHING;

    INSERT INTO tipo_agendas (nombre) VALUES
        ('Audiencia'), ('Consulta'), ('Reunión'), ('Seguimiento'), ('Diligencia')
    ON CONFLICT (nombre) DO NOTHING;

    INSERT INTO tipo_documentos (nombre) VALUES
        ('Demanda'), ('Contrato'), ('Poder'), ('Prueba'), ('Otro')
    ON CONFLICT (nombre) DO NOTHING;

    INSERT INTO vinculos (nombre) VALUES
        ('Co-Abogado'), ('Apoyo')
    ON CONFLICT (nombre) DO NOTHING;

    -- Resolución de ids de catálogo (por nombre; nunca ids literales)
    SELECT id INTO v_rol_superadmin FROM roles WHERE nombre = 'SUPER_ADMIN';
    SELECT id INTO v_rol_admin      FROM roles WHERE nombre = 'ADMINISTRADOR';
    SELECT id INTO v_rol_abogado    FROM roles WHERE nombre = 'ABOGADO';
    SELECT id INTO v_rol_secretaria FROM roles WHERE nombre = 'SECRETARIA';

    SELECT id INTO v_est_sin_definir FROM estados WHERE nombre = 'SIN DEFINIR';
    SELECT id INTO v_est_tramite     FROM estados WHERE nombre = 'EN TRÁMITE';
    SELECT id INTO v_est_espera      FROM estados WHERE nombre = 'EN ESPERA';
    SELECT id INTO v_est_terminado   FROM estados WHERE nombre = 'TERMINADO';
    SELECT id INTO v_est_cerrado     FROM estados WHERE nombre = 'CERRADO';

    SELECT id INTO v_st_resp_civil    FROM subtipo_casos WHERE id_tipo_caso = v_tc_civil     AND nombre = 'Responsabilidad Civil';
    SELECT id INTO v_st_contratos     FROM subtipo_casos WHERE id_tipo_caso = v_tc_civil     AND nombre = 'Contratos';
    SELECT id INTO v_st_lesiones      FROM subtipo_casos WHERE id_tipo_caso = v_tc_penal     AND nombre = 'Lesiones Personales';
    SELECT id INTO v_st_despido       FROM subtipo_casos WHERE id_tipo_caso = v_tc_laboral   AND nombre = 'Despido Injustificado';
    SELECT id INTO v_st_recl_salarial FROM subtipo_casos WHERE id_tipo_caso = v_tc_laboral   AND nombre = 'Reclamación Salarial';
    SELECT id INTO v_st_divorcio      FROM subtipo_casos WHERE id_tipo_caso = v_tc_familia   AND nombre = 'Divorcio';
    SELECT id INTO v_st_custodia      FROM subtipo_casos WHERE id_tipo_caso = v_tc_familia   AND nombre = 'Custodia';
    SELECT id INTO v_st_cobro         FROM subtipo_casos WHERE id_tipo_caso = v_tc_comercial AND nombre = 'Cobro de Cartera';
    SELECT id INTO v_st_tutela        FROM subtipo_casos WHERE id_tipo_caso = v_tc_admin     AND nombre = 'Acción de Tutela';

    SELECT id INTO v_ta_socio  FROM tipo_abogados WHERE nombre = 'Socio';
    SELECT id INTO v_ta_senior FROM tipo_abogados WHERE nombre = 'Asociado Senior';
    SELECT id INTO v_ta_junior FROM tipo_abogados WHERE nombre = 'Asociado Junior';

    SELECT id INTO v_tag_audiencia   FROM tipo_agendas WHERE nombre = 'Audiencia';
    SELECT id INTO v_tag_consulta    FROM tipo_agendas WHERE nombre = 'Consulta';
    SELECT id INTO v_tag_reunion     FROM tipo_agendas WHERE nombre = 'Reunión';
    SELECT id INTO v_tag_seguimiento FROM tipo_agendas WHERE nombre = 'Seguimiento';
    SELECT id INTO v_tag_diligencia  FROM tipo_agendas WHERE nombre = 'Diligencia';

    SELECT id INTO v_vinc_principal FROM vinculos WHERE nombre = 'Principal';
    SELECT id INTO v_vinc_coabogado FROM vinculos WHERE nombre = 'Co-Abogado';
    SELECT id INTO v_rel_secretario FROM tipo_relaciones_equipo WHERE nombre = 'Secretario(a)';

    -- Catálogos V1 para clientes (niveles_salariales usa la columna 'rango')
    SELECT id INTO v_tipo_nat FROM tipo_clientes WHERE nombre = 'Natural';
    SELECT id INTO v_tipo_jur FROM tipo_clientes WHERE nombre = 'Juridico';
    SELECT id INTO v_tid_cc  FROM tipo_identificaciones WHERE nombre = 'Cédula de Ciudadanía';
    SELECT id INTO v_tid_nit FROM tipo_identificaciones WHERE nombre = 'NIT';
    SELECT id INTO v_gen_m FROM generos WHERE nombre = 'Masculino';
    SELECT id INTO v_gen_f FROM generos WHERE nombre = 'Femenino';
    SELECT id INTO v_ec_soltero FROM estados_civiles WHERE nombre = 'Soltero(a)';
    SELECT id INTO v_ec_casado  FROM estados_civiles WHERE nombre = 'Casado(a)';
    SELECT id INTO v_est2 FROM estratos_economicos WHERE nombre = 'Estrato 2';
    SELECT id INTO v_est3 FROM estratos_economicos WHERE nombre = 'Estrato 3';
    SELECT id INTO v_est4 FROM estratos_economicos WHERE nombre = 'Estrato 4';
    SELECT id INTO v_viv_propia    FROM tipo_viviendas WHERE nombre = 'Propia';
    SELECT id INTO v_viv_arrendada FROM tipo_viviendas WHERE nombre = 'Arrendada';
    SELECT id INTO v_ns_1_2 FROM niveles_salariales WHERE rango = '1 - 2 SMLV';
    SELECT id INTO v_ns_3_5 FROM niveles_salariales WHERE rango = '3 - 5 SMLV';
    SELECT id INTO v_te_dep FROM tipo_empleados WHERE nombre = 'Empleado Dependiente';
    SELECT id INTO v_te_ind FROM tipo_empleados WHERE nombre = 'Independiente';
    SELECT id INTO v_te_est FROM tipo_empleados WHERE nombre = 'Estudiante';
    SELECT id INTO v_ge_uni FROM grados_escolaridad WHERE nombre = 'Universitario';
    SELECT id INTO v_ge_tec FROM grados_escolaridad WHERE nombre = 'Técnico / Tecnólogo';
    SELECT id INTO v_disc_ninguna FROM discapacidades WHERE nombre = 'Ninguna';

    -------------------------------------------------------------------------
    -- 3. Firmas demo (nunca la firma SISTEMA). 'integrante' es cosmético
    --    (nada lo mantiene en código); se fija a 5, el número real de
    --    personas de cada firma demo (sin contar al súper admin).
    --    DO UPDATE (y no DO NOTHING) para que un re-run deje integrante=5
    --    aunque la firma ya existiera con otro valor.
    -------------------------------------------------------------------------
    INSERT INTO firmas (nombre, nit, integrante) VALUES
        ('Firma Demo Norte', '9000000011', 5),
        ('Firma Demo Sur',   '9000000022', 5)
    ON CONFLICT (nit) DO UPDATE SET nombre = EXCLUDED.nombre, integrante = EXCLUDED.integrante;

    SELECT id INTO v_f_norte FROM firmas WHERE nit = '9000000011';
    SELECT id INTO v_f_sur   FROM firmas WHERE nit = '9000000022';

    -------------------------------------------------------------------------
    -- 4. Personas P01–P11. id_firma = NULL solo para la súper admin.
    --    División nombre/apellido: nombre = nombre de pila,
    --    apellido = apellidos (p. ej. 'Sofía' / 'Mendoza Ruiz').
    -------------------------------------------------------------------------
    INSERT INTO personas (nombre, apellido, cc, activo, id_rol, telefono, fecha_nacimiento, id_firma) VALUES
        ('Sofía',     'Mendoza Ruiz', '1000000001', TRUE, v_rol_superadmin, '3000000001', DATE '1988-03-15', NULL),
        ('Carlos',    'Rojas Medina', '1000000002', TRUE, v_rol_admin,      '3000000002', DATE '1985-07-22', v_f_norte),
        ('Diana',     'Páez Cárdenas','1000000003', TRUE, v_rol_admin,      '3000000003', DATE '1990-01-30', v_f_sur),
        ('Felipe',    'Navarro Ortiz','1000000004', TRUE, v_rol_admin,      '3000000004', DATE '1987-11-08', v_f_norte),
        ('Valentina', 'Castro Gil',   '1000000005', TRUE, v_rol_abogado,    '3000000005', DATE '1993-05-19', v_f_norte),
        ('Mateo',     'Silva Torres', '1000000006', TRUE, v_rol_abogado,    '3000000006', DATE '1995-09-02', v_f_norte),
        ('Camilo',    'Duarte Peña',  '1000000007', TRUE, v_rol_admin,      '3000000007', DATE '1984-12-11', v_f_sur),
        ('Isabella',  'Ramos Soto',   '1000000008', TRUE, v_rol_abogado,    '3000000008', DATE '1991-06-25', v_f_sur),
        ('Sebastián', 'Vega Luna',    '1000000009', TRUE, v_rol_abogado,    '3000000009', DATE '1996-02-14', v_f_sur),
        ('Laura',     'Gómez Niño',   '1000000010', TRUE, v_rol_secretaria, '3000000010', DATE '1994-08-07', v_f_norte),
        ('Natalia',   'Ortiz Rey',    '1000000011', TRUE, v_rol_secretaria, '3000000011', DATE '1997-04-03', v_f_sur)
    ON CONFLICT (cc) DO NOTHING;

    SELECT id INTO v_p01 FROM personas WHERE cc = '1000000001';
    SELECT id INTO v_p02 FROM personas WHERE cc = '1000000002';
    SELECT id INTO v_p03 FROM personas WHERE cc = '1000000003';
    SELECT id INTO v_p04 FROM personas WHERE cc = '1000000004';
    SELECT id INTO v_p05 FROM personas WHERE cc = '1000000005';
    SELECT id INTO v_p06 FROM personas WHERE cc = '1000000006';
    SELECT id INTO v_p07 FROM personas WHERE cc = '1000000007';
    SELECT id INTO v_p08 FROM personas WHERE cc = '1000000008';
    SELECT id INTO v_p09 FROM personas WHERE cc = '1000000009';
    SELECT id INTO v_p10 FROM personas WHERE cc = '1000000010';
    SELECT id INTO v_p11 FROM personas WHERE cc = '1000000011';

    -------------------------------------------------------------------------
    -- 5. Logins (11). Súper admin primero (coherencia con el bootstrap).
    --    Misma contraseña de prueba para todas (ver guía §5; hash §3).
    --    NO se crea login para la cuenta real de Daniel.
    -------------------------------------------------------------------------
    INSERT INTO logins (id_persona, correo, password) VALUES
        (v_p01, 'superadmin@lexdemo.local',      v_hash),
        (v_p02, 'admin.norte@lexdemo.local',     v_hash),
        (v_p03, 'admin.sur@lexdemo.local',       v_hash),
        (v_p04, 'felipe.navarro@lexdemo.local',  v_hash),
        (v_p05, 'valentina.castro@lexdemo.local',v_hash),
        (v_p06, 'mateo.silva@lexdemo.local',     v_hash),
        (v_p07, 'camilo.duarte@lexdemo.local',   v_hash),
        (v_p08, 'isabella.ramos@lexdemo.local',  v_hash),
        (v_p09, 'sebastian.vega@lexdemo.local',  v_hash),
        (v_p10, 'laura.gomez@lexdemo.local',     v_hash),
        (v_p11, 'natalia.ortiz@lexdemo.local',   v_hash)
    ON CONFLICT (id_persona) DO NOTHING;

    -------------------------------------------------------------------------
    -- 6. Abogados (6). personas.id_firma = abogados.id_firma (obligatorio:
    --    si la persona tiene registro de abogado ACTIVO, manda
    --    abogados.id_firma). P04 y P07 son abogado-admin.
    -------------------------------------------------------------------------
    INSERT INTO abogados (id_persona, id_firma, id_tipo_abogado, tarjeta_profesional, activo) VALUES
        (v_p04, v_f_norte, v_ta_senior, 'TP-100004', TRUE),
        (v_p05, v_f_norte, v_ta_junior, 'TP-100005', TRUE),
        (v_p06, v_f_norte, v_ta_junior, 'TP-100006', TRUE),
        (v_p07, v_f_sur,   v_ta_socio,  'TP-100007', TRUE),
        (v_p08, v_f_sur,   v_ta_senior, 'TP-100008', TRUE),
        (v_p09, v_f_sur,   v_ta_junior, 'TP-100009', TRUE)
    ON CONFLICT (id_persona) DO NOTHING;

    SELECT id INTO v_ab_p04 FROM abogados WHERE id_persona = v_p04;
    SELECT id INTO v_ab_p05 FROM abogados WHERE id_persona = v_p05;
    SELECT id INTO v_ab_p06 FROM abogados WHERE id_persona = v_p06;
    SELECT id INTO v_ab_p07 FROM abogados WHERE id_persona = v_p07;
    SELECT id INTO v_ab_p08 FROM abogados WHERE id_persona = v_p08;
    SELECT id INTO v_ab_p09 FROM abogados WHERE id_persona = v_p09;

    -------------------------------------------------------------------------
    -- 7. Equipo (secretarias). Sin ON CONFLICT útil (índice único parcial):
    --    guarda NOT EXISTS. Relación 'Secretario(a)' (ya en V1).
    -------------------------------------------------------------------------
    INSERT INTO equipo_abogados (id_abogado, id_persona, id_tipo_relacion, fecha_asignacion, activo)
    SELECT v_ab_p04, v_p10, v_rel_secretario, CURRENT_DATE, TRUE
    WHERE NOT EXISTS (SELECT 1 FROM equipo_abogados
                      WHERE id_abogado = v_ab_p04 AND id_persona = v_p10 AND activo = TRUE);

    INSERT INTO equipo_abogados (id_abogado, id_persona, id_tipo_relacion, fecha_asignacion, activo)
    SELECT v_ab_p07, v_p11, v_rel_secretario, CURRENT_DATE, TRUE
    WHERE NOT EXISTS (SELECT 1 FROM equipo_abogados
                      WHERE id_abogado = v_ab_p07 AND id_persona = v_p11 AND activo = TRUE);

    -------------------------------------------------------------------------
    -- 8. Clientes (7). Pre-vuelo propio: la identificación es UNIQUE
    --    compuesta con la firma; si ya existe en la firma demo con otros
    --    datos, abortar (no pisar un cliente real).
    -------------------------------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM clientes c WHERE
            (c.identificacion = '2000000001' AND c.id_firma = v_f_norte AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Mariana Torres', 'Gómez'))
         OR (c.identificacion = '2000000002' AND c.id_firma = v_f_norte AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Jorge Ramírez', 'Pardo'))
         OR (c.identificacion = '2000000003' AND c.id_firma = v_f_norte AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Lucía Fernández', 'Díaz'))
         OR (c.identificacion = '2000000004' AND c.id_firma = v_f_norte AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Comercializadora Andina', 'S.A.S.'))
         OR (c.identificacion = '2000000005' AND c.id_firma = v_f_sur   AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Andrés Molina', 'Vega'))
         OR (c.identificacion = '2000000006' AND c.id_firma = v_f_sur   AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Paola Herrera', 'León'))
         OR (c.identificacion = '2000000007' AND c.id_firma = v_f_sur   AND (c.nombres, c.primer_apellido) IS DISTINCT FROM ('Distribuidora del Caribe', 'S.A.S.'))
    ) THEN
        RAISE EXCEPTION 'Pre-vuelo: una identificación de cliente del seed ya existe en la firma demo con OTROS datos. Cambia el rango del seed antes de ejecutar.';
    END IF;

    -- CN1–CN4 (Norte). Campos de relleno (género, estado civil, estrato,
    -- vivienda, nivel salarial, tipo empleado, grado, discapacidad) con
    -- valores V1 para que el demo se vea completo. Teléfonos ficticios.
    INSERT INTO clientes (id_firma, id_tipo_cliente, fecha_vinculacion, activo,
        identificacion, id_tipo_identificacion, nombres, primer_apellido, segundo_apellido,
        id_genero, id_estado_civil, fecha_nacimiento, lugar_nacimiento, nacionalidad,
        ciudad_residencia, localidad_sector, barrio, direccion_residencia,
        telefono_residencia, direccion_oficina, telefono_oficina, telefono_movil, correo,
        id_estrato, id_vivienda, id_nivel_salarial, id_tipo_empleado, id_grado_escolaridad, id_discapacidad)
    VALUES
        (v_f_norte, v_tipo_nat, DATE '2026-09-10', TRUE,
         '2000000001', v_tid_cc, 'Mariana Torres', 'Gómez', NULL,
         v_gen_f, v_ec_soltero, DATE '1990-04-12', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Centro', 'El Prado', 'Calle 10 # 5-20',
         NULL, NULL, NULL, '3000000101', 'mariana.torres@lexdemo.local',
         v_est3, v_viv_propia, v_ns_1_2, v_te_dep, v_ge_uni, v_disc_ninguna),
        (v_f_norte, v_tipo_nat, DATE '2026-09-15', TRUE,
         '2000000002', v_tid_cc, 'Jorge Ramírez', 'Pardo', NULL,
         v_gen_m, v_ec_casado, DATE '1982-10-05', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Centro', 'Bavaria', 'Carrera 4 # 12-35',
         NULL, NULL, NULL, '3000000102', 'jorge.ramirez@lexdemo.local',
         v_est4, v_viv_propia, v_ns_3_5, v_te_ind, v_ge_uni, v_disc_ninguna),
        (v_f_norte, v_tipo_nat, DATE '2026-09-20', TRUE,
         '2000000003', v_tid_cc, 'Lucía Fernández', 'Díaz', NULL,
         v_gen_f, v_ec_soltero, DATE '1995-01-28', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Centro', 'Los Almendros', 'Calle 22 # 8-10',
         NULL, NULL, NULL, '3000000103', 'lucia.fernandez@lexdemo.local',
         v_est2, v_viv_arrendada, v_ns_1_2, v_te_est, v_ge_tec, v_disc_ninguna),
        -- CN4 Jurídico: primer_apellido = 'S.A.S.' porque el servicio lo
        -- exige no-blanco también para Jurídico (diseño §8.6). Fecha de
        -- nacimiento = fecha de constitución de la sociedad (§11.8).
        (v_f_norte, v_tipo_jur, DATE '2026-09-25', TRUE,
         '2000000004', v_tid_nit, 'Comercializadora Andina', 'S.A.S.', NULL,
         NULL, NULL, DATE '2018-06-01', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Centro', 'El Prado', NULL,
         NULL, 'Calle 10 # 5-20', '3000000104', '3000000104', 'contacto.andina@lexdemo.local',
         NULL, NULL, NULL, NULL, NULL, v_disc_ninguna)
    ON CONFLICT (identificacion, id_firma) DO NOTHING;

    -- CS1–CS3 (Sur)
    INSERT INTO clientes (id_firma, id_tipo_cliente, fecha_vinculacion, activo,
        identificacion, id_tipo_identificacion, nombres, primer_apellido, segundo_apellido,
        id_genero, id_estado_civil, fecha_nacimiento, lugar_nacimiento, nacionalidad,
        ciudad_residencia, localidad_sector, barrio, direccion_residencia,
        telefono_residencia, direccion_oficina, telefono_oficina, telefono_movil, correo,
        id_estrato, id_vivienda, id_nivel_salarial, id_tipo_empleado, id_grado_escolaridad, id_discapacidad)
    VALUES
        (v_f_sur, v_tipo_nat, DATE '2026-10-02', TRUE,
         '2000000005', v_tid_cc, 'Andrés Molina', 'Vega', NULL,
         v_gen_m, v_ec_soltero, DATE '1988-12-19', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Sur', 'La Esperanza', 'Carrera 19 # 30-15',
         NULL, NULL, NULL, '3000000105', 'andres.molina@lexdemo.local',
         v_est3, v_viv_propia, v_ns_1_2, v_te_dep, v_ge_uni, v_disc_ninguna),
        (v_f_sur, v_tipo_nat, DATE '2026-10-06', TRUE,
         '2000000006', v_tid_cc, 'Paola Herrera', 'León', NULL,
         v_gen_f, v_ec_soltero, DATE '1992-07-08', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Sur', 'El Pando', 'Calle 30 # 19-40',
         NULL, NULL, NULL, '3000000106', 'paola.herrera@lexdemo.local',
         v_est3, v_viv_arrendada, v_ns_1_2, v_te_dep, v_ge_uni, v_disc_ninguna),
        (v_f_sur, v_tipo_jur, DATE '2026-10-09', TRUE,
         '2000000007', v_tid_nit, 'Distribuidora del Caribe', 'S.A.S.', NULL,
         NULL, NULL, DATE '2019-03-15', 'Santa Marta', 'Colombiana',
         'Santa Marta', 'Sur', 'La Esperanza', NULL,
         NULL, 'Carrera 19 # 30-15', '3000000107', '3000000107', 'contacto.caribe@lexdemo.local',
         NULL, NULL, NULL, NULL, NULL, v_disc_ninguna)
    ON CONFLICT (identificacion, id_firma) DO NOTHING;

    SELECT id INTO v_c_cn1 FROM clientes WHERE identificacion = '2000000001' AND id_firma = v_f_norte;
    SELECT id INTO v_c_cn2 FROM clientes WHERE identificacion = '2000000002' AND id_firma = v_f_norte;
    SELECT id INTO v_c_cn3 FROM clientes WHERE identificacion = '2000000003' AND id_firma = v_f_norte;
    SELECT id INTO v_c_cn4 FROM clientes WHERE identificacion = '2000000004' AND id_firma = v_f_norte;
    SELECT id INTO v_c_cs1 FROM clientes WHERE identificacion = '2000000005' AND id_firma = v_f_sur;
    SELECT id INTO v_c_cs2 FROM clientes WHERE identificacion = '2000000006' AND id_firma = v_f_sur;
    SELECT id INTO v_c_cs3 FROM clientes WHERE identificacion = '2000000007' AND id_firma = v_f_sur;

    -------------------------------------------------------------------------
    -- 9. Casos (10). Radicados SOLO por nextval('radicado_seq') (diseño §4):
    --    un nextval por caso, mismo valor en radicado_interno (10 dígitos)
    --    y radicado_judicial (23 dígitos). Nunca literales.
    --    Idempotencia por guarda NOT EXISTS sobre (titulo, id_firma):
    --    en un re-run NO se consume otro nextval.
    --    Padres primero; los hijos (N2, S2) después. id_caso_padre = NULL
    --    deja que el trigger trg_set_root_case los cuelgue del maestro (id 1).
    --    N4/N5/S5 están en estado TERMINADO/CERRADO: solo existen vía SQL,
    --    la API no permite crear casos en estado terminal (diseño §11.4).
    -------------------------------------------------------------------------
    -- N1 — Norte, padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Cobro ejecutivo de cartera — Comercializadora Andina' AND id_firma = v_f_norte) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Cobro ejecutivo de cartera — Comercializadora Andina',
               'Cobro ejecutivo de cartera a favor de Comercializadora Andina S.A.S.',
               v_est_tramite, v_st_cobro, DATE '2026-09-15', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_norte
        FROM n RETURNING id INTO v_caso_n1;
    ELSE
        SELECT id INTO v_caso_n1 FROM casos WHERE titulo = 'Cobro ejecutivo de cartera — Comercializadora Andina' AND id_firma = v_f_norte;
    END IF;

    -- N3 — Norte, padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Divorcio contencioso' AND id_firma = v_f_norte) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Divorcio contencioso',
               'Proceso de divorcio contencioso con liquidación de sociedad conyugal.',
               v_est_espera, v_st_divorcio, DATE '2026-10-10', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_norte
        FROM n RETURNING id INTO v_caso_n3;
    ELSE
        SELECT id INTO v_caso_n3 FROM casos WHERE titulo = 'Divorcio contencioso' AND id_firma = v_f_norte;
    END IF;

    -- N4 — Norte, TERMINADO (solo vía SQL), padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Responsabilidad civil — accidente de tránsito' AND id_firma = v_f_norte) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Responsabilidad civil — accidente de tránsito',
               'Reclamación por daños y perjuicios derivados de accidente de tránsito.',
               v_est_terminado, v_st_resp_civil, DATE '2026-08-20', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_norte
        FROM n RETURNING id INTO v_caso_n4;
    ELSE
        SELECT id INTO v_caso_n4 FROM casos WHERE titulo = 'Responsabilidad civil — accidente de tránsito' AND id_firma = v_f_norte;
    END IF;

    -- N5 — Norte, CERRADO (solo vía SQL), padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Acción de tutela — derecho a la salud' AND id_firma = v_f_norte) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Acción de tutela — derecho a la salud',
               'Acción de tutela por vulneración del derecho fundamental a la salud.',
               v_est_cerrado, v_st_tutela, DATE '2026-09-01', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_norte
        FROM n RETURNING id INTO v_caso_n5;
    ELSE
        SELECT id INTO v_caso_n5 FROM casos WHERE titulo = 'Acción de tutela — derecho a la salud' AND id_firma = v_f_norte;
    END IF;

    -- S1 — Sur, padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Contrato de arrendamiento incumplido' AND id_firma = v_f_sur) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Contrato de arrendamiento incumplido',
               'Incumplimiento de contrato de arrendamiento de local comercial.',
               v_est_tramite, v_st_contratos, DATE '2026-09-22', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_sur
        FROM n RETURNING id INTO v_caso_s1;
    ELSE
        SELECT id INTO v_caso_s1 FROM casos WHERE titulo = 'Contrato de arrendamiento incumplido' AND id_firma = v_f_sur;
    END IF;

    -- S3 — Sur, padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Custodia de menores' AND id_firma = v_f_sur) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Custodia de menores',
               'Proceso de custodia y cuidado personal de menores de edad.',
               v_est_espera, v_st_custodia, DATE '2026-10-12', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_sur
        FROM n RETURNING id INTO v_caso_s3;
    ELSE
        SELECT id INTO v_caso_s3 FROM casos WHERE titulo = 'Custodia de menores' AND id_firma = v_f_sur;
    END IF;

    -- S4 — Sur, SIN DEFINIR, padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Lesiones personales — riña' AND id_firma = v_f_sur) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Lesiones personales — riña',
               'Proceso penal por lesiones personales derivadas de riña.',
               v_est_sin_definir, v_st_lesiones, DATE '2026-11-03', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_sur
        FROM n RETURNING id INTO v_caso_s4;
    ELSE
        SELECT id INTO v_caso_s4 FROM casos WHERE titulo = 'Lesiones personales — riña' AND id_firma = v_f_sur;
    END IF;

    -- S5 — Sur, TERMINADO (solo vía SQL), padre (maestro)
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Cobro de cartera — Distribuidora del Caribe' AND id_firma = v_f_sur) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Cobro de cartera — Distribuidora del Caribe',
               'Cobro de cartera vencida a favor de Distribuidora del Caribe S.A.S.',
               v_est_terminado, v_st_cobro, DATE '2026-08-28', TRUE, NULL,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_sur
        FROM n RETURNING id INTO v_caso_s5;
    ELSE
        SELECT id INTO v_caso_s5 FROM casos WHERE titulo = 'Cobro de cartera — Distribuidora del Caribe' AND id_firma = v_f_sur;
    END IF;

    -- N2 — Norte, HIJO de N1
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Reclamación salarial — exempleado' AND id_firma = v_f_norte) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Reclamación salarial — exempleado',
               'Reclamación de salarios y prestaciones adeudadas a exempleado.',
               v_est_tramite, v_st_recl_salarial, DATE '2026-10-02', TRUE, v_caso_n1,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_norte
        FROM n RETURNING id INTO v_caso_n2;
    ELSE
        SELECT id INTO v_caso_n2 FROM casos WHERE titulo = 'Reclamación salarial — exempleado' AND id_firma = v_f_norte;
    END IF;

    -- S2 — Sur, HIJO de S1
    IF NOT EXISTS (SELECT 1 FROM casos WHERE titulo = 'Despido injustificado' AND id_firma = v_f_sur) THEN
        WITH n AS (SELECT nextval('radicado_seq') AS v)
        INSERT INTO casos (titulo, descripcion, id_estado, id_subtipo_caso, fecha_inicio, activo, id_caso_padre, radicado_interno, radicado_judicial, id_firma)
        SELECT 'Despido injustificado',
               'Demanda por despido sin justa causa y reintegro de prestaciones.',
               v_est_tramite, v_st_despido, DATE '2026-10-05', TRUE, v_caso_s1,
               lpad(n.v::text, 10, '0'), lpad(n.v::text, 23, '0'), v_f_sur
        FROM n RETURNING id INTO v_caso_s2;
    ELSE
        SELECT id INTO v_caso_s2 FROM casos WHERE titulo = 'Despido injustificado' AND id_firma = v_f_sur;
    END IF;

    -------------------------------------------------------------------------
    -- 10. Vínculos (12). Primero los 10 Principal AUTORIZADO (uno por caso,
    --     como los crea CasoServiceImpl.crearVinculo). 'autorizado' (columna
    --     legacy) coherente con 'estado': TRUE solo si AUTORIZADO.
    --     El trigger trg_un_solo_principal impide un segundo Principal por
    --     caso (aunque fuera PENDIENTE): por eso los vínculos adicionales
    --     usan Co-Abogado, nunca Principal.
    -------------------------------------------------------------------------
    INSERT INTO vinculo_casos (id_persona_firma, id_cliente, id_caso, id_vinculo, autorizado, estado) VALUES
        (v_p04, v_c_cn4, v_caso_n1, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p05, v_c_cn1, v_caso_n2, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p06, v_c_cn2, v_caso_n3, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p04, v_c_cn3, v_caso_n4, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p05, v_c_cn1, v_caso_n5, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p07, v_c_cs1, v_caso_s1, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p08, v_c_cs2, v_caso_s2, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p09, v_c_cs3, v_caso_s3, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p08, v_c_cs1, v_caso_s4, v_vinc_principal, TRUE, 'AUTORIZADO'),
        (v_p07, v_c_cs3, v_caso_s5, v_vinc_principal, TRUE, 'AUTORIZADO')
    ON CONFLICT (id_persona_firma, id_cliente, id_caso) DO NOTHING;

    -- V-P1: el vínculo PENDIENTE del demo (N3, Valentina como Co-Abogado).
    INSERT INTO vinculo_casos (id_persona_firma, id_cliente, id_caso, id_vinculo, autorizado, estado) VALUES
        (v_p05, v_c_cn2, v_caso_n3, v_vinc_coabogado, FALSE, 'PENDIENTE')
    ON CONFLICT (id_persona_firma, id_cliente, id_caso) DO NOTHING;

    -- V-R1: el vínculo RECHAZADO del demo (N5, Mateo como Co-Abogado).
    INSERT INTO vinculo_casos (id_persona_firma, id_cliente, id_caso, id_vinculo, autorizado, estado) VALUES
        (v_p06, v_c_cn1, v_caso_n5, v_vinc_coabogado, FALSE, 'RECHAZADO')
    ON CONFLICT (id_persona_firma, id_cliente, id_caso) DO NOTHING;

    -------------------------------------------------------------------------
    -- 11. Agendas (11). TODAS con id_caso (la columna es NOT NULL: una
    --     agenda sin caso es imposible en este modelo, diseño §11.3).
    --     Fechas RELATIVAS a CURRENT_DATE para que el seed siga teniendo
    --     pasado/futuro se ejecute cuando se ejecute. Sin doble reserva:
    --     ningún abogado repite fecha/hora. A2, A5 y A8 son pasadas
    --     (solo vía SQL: la API exige fecha futura, diseño §11.5);
    --     A8 además está cancelada (activa = FALSE).
    --     Guarda de idempotencia: la clave estable es (abogado, tipo,
    --     caso, descripción); la guarda NO usa fecha_exp porque las
    --     fechas son relativas a CURRENT_DATE y en un re-run otro día
    --     serían distintas y duplicarían las agendas (desviación del
    --     diseño §10.2, que proponía guardar por fecha_exp).
    -------------------------------------------------------------------------
    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p04, v_tag_audiencia, TRUE, v_caso_n1, 'Audiencia inicial del cobro ejecutivo.',
           (CURRENT_DATE + INTERVAL '3 days') + TIME '09:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p04 AND id_caso = v_caso_n1
                      AND id_tipo_agenda = v_tag_audiencia AND descripcion = 'Audiencia inicial del cobro ejecutivo.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p04, v_tag_seguimiento, TRUE, v_caso_n4, 'Seguimiento del caso de responsabilidad civil.',
           (CURRENT_DATE - INTERVAL '5 days') + TIME '10:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p04 AND id_caso = v_caso_n4
                      AND id_tipo_agenda = v_tag_seguimiento AND descripcion = 'Seguimiento del caso de responsabilidad civil.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p05, v_tag_consulta, TRUE, v_caso_n2, 'Consulta inicial con el cliente por reclamación salarial.',
           (CURRENT_DATE + INTERVAL '2 days') + TIME '14:30'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p05 AND id_caso = v_caso_n2
                      AND id_tipo_agenda = v_tag_consulta AND descripcion = 'Consulta inicial con el cliente por reclamación salarial.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p05, v_tag_reunion, TRUE, v_caso_n5, 'Reunión de seguimiento de la acción de tutela.',
           (CURRENT_DATE + INTERVAL '7 days') + TIME '11:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p05 AND id_caso = v_caso_n5
                      AND id_tipo_agenda = v_tag_reunion AND descripcion = 'Reunión de seguimiento de la acción de tutela.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p06, v_tag_audiencia, TRUE, v_caso_n3, 'Audiencia del proceso de divorcio.',
           (CURRENT_DATE - INTERVAL '2 days') + TIME '16:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p06 AND id_caso = v_caso_n3
                      AND id_tipo_agenda = v_tag_audiencia AND descripcion = 'Audiencia del proceso de divorcio.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p06, v_tag_diligencia, TRUE, v_caso_n3, 'Diligencia de notificación en el divorcio.',
           (CURRENT_DATE + INTERVAL '5 days') + TIME '09:30'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p06 AND id_caso = v_caso_n3
                      AND id_tipo_agenda = v_tag_diligencia AND descripcion = 'Diligencia de notificación en el divorcio.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p07, v_tag_reunion, TRUE, v_caso_s1, 'Reunión con el arrendador por contrato incumplido.',
           (CURRENT_DATE + INTERVAL '4 days') + TIME '10:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p07 AND id_caso = v_caso_s1
                      AND id_tipo_agenda = v_tag_reunion AND descripcion = 'Reunión con el arrendador por contrato incumplido.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p07, v_tag_seguimiento, FALSE, v_caso_s5, 'Seguimiento del cobro de cartera (cancelada).',
           (CURRENT_DATE - INTERVAL '3 days') + TIME '15:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p07 AND id_caso = v_caso_s5
                      AND id_tipo_agenda = v_tag_seguimiento AND descripcion = 'Seguimiento del cobro de cartera (cancelada).');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p08, v_tag_audiencia, TRUE, v_caso_s2, 'Audiencia del proceso por despido injustificado.',
           (CURRENT_DATE + INTERVAL '2 days') + TIME '09:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p08 AND id_caso = v_caso_s2
                      AND id_tipo_agenda = v_tag_audiencia AND descripcion = 'Audiencia del proceso por despido injustificado.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p08, v_tag_consulta, TRUE, v_caso_s4, 'Consulta inicial por lesiones personales.',
           (CURRENT_DATE + INTERVAL '9 days') + TIME '16:30'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p08 AND id_caso = v_caso_s4
                      AND id_tipo_agenda = v_tag_consulta AND descripcion = 'Consulta inicial por lesiones personales.');

    INSERT INTO agendas (id_abogado, id_tipo_agenda, activa, id_caso, descripcion, fecha_exp)
    SELECT v_ab_p09, v_tag_audiencia, TRUE, v_caso_s3, 'Audiencia del proceso de custodia de menores.',
           (CURRENT_DATE + INTERVAL '6 days') + TIME '14:00'
    WHERE NOT EXISTS (SELECT 1 FROM agendas WHERE id_abogado = v_ab_p09 AND id_caso = v_caso_s3
                      AND id_tipo_agenda = v_tag_audiencia AND descripcion = 'Audiencia del proceso de custodia de menores.');

    RAISE NOTICE 'Siembra completada: 2 firmas, 11 personas, 11 logins, 6 abogados, 2 equipo, 7 clientes, 10 casos, 12 vínculos, 11 agendas.';
END $$;

-- ----------------------------------------------------------------------------
-- 12. VERIFICACIÓN (SELECTs; deben dar los valores esperados o 0 filas)
-- ----------------------------------------------------------------------------
-- Conteos del demo (esperado entre paréntesis)
SELECT 'firmas_demo'    AS tabla, COUNT(*) AS cantidad, 2  AS esperado FROM firmas WHERE nit IN ('9000000011','9000000022')
UNION ALL SELECT 'firmas_total', COUNT(*), 3 FROM firmas
UNION ALL SELECT 'personas_demo', COUNT(*), 11 FROM personas WHERE cc IN ('1000000001','1000000002','1000000003','1000000004','1000000005','1000000006','1000000007','1000000008','1000000009','1000000010','1000000011')
UNION ALL SELECT 'logins_demo', COUNT(*), 11 FROM logins WHERE correo LIKE '%@lexdemo.local'
UNION ALL SELECT 'abogados_demo', COUNT(*), 6 FROM abogados WHERE id_persona IN (SELECT id FROM personas WHERE cc IN ('1000000004','1000000005','1000000006','1000000007','1000000008','1000000009'))
UNION ALL SELECT 'equipo_demo', COUNT(*), 2 FROM equipo_abogados WHERE id_persona IN (SELECT id FROM personas WHERE cc IN ('1000000010','1000000011'))
UNION ALL SELECT 'clientes_demo', COUNT(*), 7 FROM clientes WHERE identificacion IN ('2000000001','2000000002','2000000003','2000000004','2000000005','2000000006','2000000007') AND id_firma IN (SELECT id FROM firmas WHERE nit IN ('9000000011','9000000022'))
UNION ALL SELECT 'casos_demo', COUNT(*), 10 FROM casos WHERE id > 1 AND titulo <> 'CASO MAESTRO SISTEMA' AND id_firma IN (SELECT id FROM firmas WHERE nit IN ('9000000011','9000000022'))
UNION ALL SELECT 'casos_total', COUNT(*), 11 FROM casos
UNION ALL SELECT 'vinculos_demo', COUNT(*), 12 FROM vinculo_casos WHERE id_caso IN (SELECT id FROM casos WHERE id_firma IN (SELECT id FROM firmas WHERE nit IN ('9000000011','9000000022')))
UNION ALL SELECT 'agendas_demo', COUNT(*), 11 FROM agendas WHERE id_caso IN (SELECT id FROM casos WHERE id_firma IN (SELECT id FROM firmas WHERE nit IN ('9000000011','9000000022')));

-- Un solo vínculo Principal por caso (debe devolver 0 filas)
SELECT c.id AS id_caso, c.titulo, COUNT(*) AS n_principal
FROM vinculo_casos vc
JOIN casos c ON c.id = vc.id_caso
WHERE vc.id_vinculo = (SELECT id FROM vinculos WHERE nombre = 'Principal')
GROUP BY c.id, c.titulo
HAVING COUNT(*) <> 1;

-- Ningún caso demo cuelga de un padre de otra firma (salvo el maestro) (0 filas)
SELECT c.id AS id_caso, c.titulo
FROM casos c
JOIN casos p ON p.id = c.id_caso_padre
WHERE c.id > 1 AND p.id <> 1 AND c.id_firma <> p.id_firma;

-- Radicados con formato: interno 10 dígitos, judicial 23 dígitos (0 filas)
SELECT id, titulo, radicado_interno, radicado_judicial
FROM casos
WHERE id > 1
  AND id_firma IN (SELECT id FROM firmas WHERE nit IN ('9000000011','9000000022'))
  AND (radicado_interno !~ '^\d{10}$' OR radicado_judicial !~ '^\d{23}$');

-- Cuentas de prueba sembradas (11 filas)
SELECT l.correo, r.nombre AS rol, p.nombre || ' ' || p.apellido AS persona,
       f.nombre AS firma
FROM logins l
JOIN personas p ON p.id = l.id_persona
JOIN roles r ON r.id = p.id_rol
LEFT JOIN firmas f ON f.id = p.id_firma
WHERE l.correo LIKE '%@lexdemo.local'
ORDER BY l.correo;

COMMIT;
