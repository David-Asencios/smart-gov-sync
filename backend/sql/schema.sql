CREATE TABLE oficinas (
    id_oficina SERIAL PRIMARY KEY,
    codigo_oficina VARCHAR(15) NOT NULL,
    siglas_oficiales VARCHAR(10),
    nombre_unidad VARCHAR(100) NOT NULL,
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE tipos_documentos (
    id_tipo_documento SERIAL PRIMARY KEY,
    nombre_tipo_documento VARCHAR(100) NOT NULL,
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE administrados (
    id_administrado SERIAL PRIMARY KEY,
    codigo_administrado VARCHAR(20),
    dni_ruc VARCHAR(15) NOT NULL,
    nombre_razon_social VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    correo_notificaciones VARCHAR(100),
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE personal_especialistas (
    id_empleado SERIAL PRIMARY KEY,
    codigo_empleado VARCHAR(20),
    nombre_completo VARCHAR(150) NOT NULL,
    cargo VARCHAR(100),
    id_oficina INT REFERENCES oficinas(id_oficina),
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE administrados_direcciones (
    id_direccion SERIAL PRIMARY KEY,
    id_administrado INT REFERENCES administrados(id_administrado),
    tipo_inmueble VARCHAR(50),
    calle VARCHAR(100),
    numero VARCHAR(10),
    comuna_distrito VARCHAR(100),
    ciudad VARCHAR(50),
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE expedientes_generales (
    id_expediente SERIAL PRIMARY KEY,
    nro_expediente_anual VARCHAR(20) NOT NULL,
    fecha_hora_apertura TIMESTAMP DEFAULT NOW(),
    asunto_general TEXT,
    estado_global VARCHAR(30) DEFAULT 'ABIERTO',
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE documentos_ingresados (
    id_documento SERIAL PRIMARY KEY,
    nro_documento_unico VARCHAR(30),
    id_expediente INT REFERENCES expedientes_generales(id_expediente),
    id_tipo_documento INT REFERENCES tipos_documentos(id_tipo_documento),
    id_administrado INT REFERENCES administrados(id_administrado),
    cantidad_folios INT,
    fecha_hora_recepcion TIMESTAMP DEFAULT NOW(),
    ruta_foto TEXT,
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE hojas_ruta_derivaciones (
    id_derivacion SERIAL PRIMARY KEY,
    codigo_barras_seguimiento VARCHAR(20),
    id_documento INT REFERENCES documentos_ingresados(id_documento),
    id_empleado_asignado INT REFERENCES personal_especialistas(id_empleado),
    id_oficina_procedencia INT REFERENCES oficinas(id_oficina),
    fecha_hora_despacho TIMESTAMP DEFAULT NOW(),
    prioridad_envio VARCHAR(20) DEFAULT 'NORMAL',
    fecha_hora_recepcion TIMESTAMP,
    observaciones_receptor TEXT,
    estado_derivacion VARCHAR(20) DEFAULT 'PENDIENTE',
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE archivo_fisico_central (
    id_ubicacion SERIAL PRIMARY KEY,
    codigo_almacen VARCHAR(20),
    nro_pabellon INT,
    nro_estante INT,
    nro_caja_fisica INT,
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE actas_archivamiento (
    id_acta SERIAL PRIMARY KEY,
    nro_acta_unico VARCHAR(20),
    id_derivacion INT REFERENCES hojas_ruta_derivaciones(id_derivacion),
    id_ubicacion_archivo INT REFERENCES archivo_fisico_central(id_ubicacion),
    fecha_hora_guardado TIMESTAMP DEFAULT NOW(),
    costo_digitalizacion DECIMAL(10,2),
    costo_arancel_custodia DECIMAL(10,2),
    costo_final_procesamiento DECIMAL(10,2),
    updated_at BIGINT DEFAULT 0
);

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL CHECK (rol IN ('ADMIN', 'MESA_PARTES', 'ESPECIALISTA', 'ARCHIVO')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_empleado INT REFERENCES personal_especialistas(id_empleado),
    updated_at BIGINT DEFAULT 0
);
