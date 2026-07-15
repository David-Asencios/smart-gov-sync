require("dotenv").config();
const bcrypt = require("bcryptjs");
const pool = require("./db");

const users = [
  { username: "admin", password: "admin123", rol: "ADMIN" },
  { username: "mesa_partes", password: "mesa123", rol: "MESA_PARTES" },
  { username: "especialista", password: "especialista123", rol: "ESPECIALISTA" },
  { username: "archivo", password: "archivo123", rol: "ARCHIVO" }
];

async function main() {
  const client = await pool.connect();
  const now = Date.now();
  try {
    await client.query("begin");

    await client.query(`
      insert into oficinas (codigo_oficina, siglas_oficiales, nombre_unidad, updated_at)
      select * from (values
        ('OF-001', 'MDP', 'Mesa de Partes', $1::bigint),
        ('OF-002', 'SG', 'Secretaria General', $1::bigint),
        ('OF-003', 'AC', 'Archivo Central', $1::bigint)
      ) as seed(codigo_oficina, siglas_oficiales, nombre_unidad, updated_at)
      where not exists (select 1 from oficinas where oficinas.codigo_oficina = seed.codigo_oficina)
    `, [now]);

    await client.query(`
      insert into tipos_documentos (nombre_tipo_documento, updated_at)
      select * from (values
        ('Solicitud', $1::bigint),
        ('Oficio', $1::bigint),
        ('Informe', $1::bigint)
      ) as seed(nombre_tipo_documento, updated_at)
      where not exists (
        select 1 from tipos_documentos
        where lower(tipos_documentos.nombre_tipo_documento) = lower(seed.nombre_tipo_documento)
      )
    `, [now]);

    await client.query(`
      insert into administrados (codigo_administrado, dni_ruc, nombre_razon_social, telefono, correo_notificaciones, updated_at)
      select 'ADM-001', '12345678', 'Maria Torres Quispe', '987654321', 'maria.torres@example.com', $1
      where not exists (select 1 from administrados where dni_ruc = '12345678')
    `, [now]);

    const office = (await client.query("select id_oficina from oficinas where codigo_oficina = 'OF-002' limit 1")).rows[0];
    const admin = (await client.query("select id_administrado from administrados where dni_ruc = '12345678' limit 1")).rows[0];
    const documentType = (await client.query("select id_tipo_documento from tipos_documentos where lower(nombre_tipo_documento) = 'solicitud' limit 1")).rows[0];

    await client.query(`
      insert into personal_especialistas (codigo_empleado, nombre_completo, cargo, id_oficina, updated_at)
      select 'EMP-001', 'Carlos Mendoza Ruiz', 'Especialista de Tramite', $1, $2
      where not exists (select 1 from personal_especialistas where codigo_empleado = 'EMP-001')
    `, [office.id_oficina, now]);
    const specialist = (await client.query("select id_empleado from personal_especialistas where codigo_empleado = 'EMP-001' limit 1")).rows[0];

    await client.query(`
      insert into administrados_direcciones (id_administrado, tipo_inmueble, calle, numero, comuna_distrito, ciudad, updated_at)
      select $1, 'Domicilio', 'Av. Grau', '245', 'Huacho', 'Huaura', $2
      where not exists (
        select 1 from administrados_direcciones
        where id_administrado = $1 and calle = 'Av. Grau' and numero = '245'
      )
    `, [admin.id_administrado, now]);

    await client.query(`
      insert into expedientes_generales (nro_expediente_anual, fecha_hora_apertura, asunto_general, estado_global, updated_at)
      select 'EXP-2026-001', now(), 'Solicitud de constancia administrativa', 'ABIERTO', $1
      where not exists (select 1 from expedientes_generales where nro_expediente_anual = 'EXP-2026-001')
    `, [now]);
    const expediente = (await client.query("select id_expediente from expedientes_generales where nro_expediente_anual = 'EXP-2026-001' limit 1")).rows[0];

    await client.query(`
      insert into documentos_ingresados (nro_documento_unico, id_expediente, id_tipo_documento, id_administrado, cantidad_folios, fecha_hora_recepcion, updated_at)
      select 'DOC-2026-001', $1, $2, $3, 3, now(), $4
      where not exists (select 1 from documentos_ingresados where nro_documento_unico = 'DOC-2026-001')
    `, [expediente.id_expediente, documentType.id_tipo_documento, admin.id_administrado, now]);
    const document = (await client.query("select id_documento from documentos_ingresados where nro_documento_unico = 'DOC-2026-001' limit 1")).rows[0];

    await client.query(`
      insert into hojas_ruta_derivaciones (
        codigo_barras_seguimiento, id_documento, id_empleado_asignado,
        id_oficina_procedencia, fecha_hora_despacho, prioridad_envio,
        observaciones_receptor, estado_derivacion, latitud, longitud, updated_at
      )
      select 'HR-2026-001', $1, $2, $3, now(), 'ALTA',
        'Verificar documentacion y responder al administrado', 'PENDIENTE',
        -12.0464, -77.0428, $4
      where not exists (select 1 from hojas_ruta_derivaciones where codigo_barras_seguimiento = 'HR-2026-001')
    `, [document.id_documento, specialist.id_empleado, office.id_oficina, now]);

    await client.query(`
      insert into archivo_fisico_central (codigo_almacen, nro_pabellon, nro_estante, nro_caja_fisica, updated_at)
      select 'ALM-01', 1, 2, 10, $1
      where not exists (select 1 from archivo_fisico_central where codigo_almacen = 'ALM-01')
    `, [now]);

    for (const user of users) {
      const passwordHash = await bcrypt.hash(user.password, 10);
      await client.query(`
        insert into usuario (username, password_hash, rol, activo, updated_at)
        values ($1, $2, $3, true, $4)
        on conflict (username)
        do update set password_hash = excluded.password_hash, rol = excluded.rol, activo = true, updated_at = excluded.updated_at
      `, [user.username, passwordHash, user.rol, now]);
    }
    await client.query(
      "update usuario set id_empleado = $1 where username = 'especialista'",
      [specialist.id_empleado]
    );

    await client.query("commit");
    console.log("Datos esenciales y usuarios de demostracion preparados correctamente");
  } catch (error) {
    await client.query("rollback");
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch(async error => {
  console.error(error);
  await pool.end().catch(() => {});
  process.exit(1);
});
