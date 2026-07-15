const express = require("express");
const pool = require("../db");
const { canRead, canWrite } = require("../access-control");

const router = express.Router();
const MAX_BATCH_SIZE = 500;

const tables = [
  { name: "oficinas", id: "id_oficina", fields: ["codigo_oficina", "siglas_oficiales", "nombre_unidad", "updated_at"] },
  { name: "tipos_documentos", id: "id_tipo_documento", fields: ["nombre_tipo_documento", "updated_at"] },
  { name: "administrados", id: "id_administrado", fields: ["codigo_administrado", "dni_ruc", "nombre_razon_social", "telefono", "correo_notificaciones", "updated_at"] },
  { name: "personal_especialistas", id: "id_empleado", fields: ["codigo_empleado", "nombre_completo", "cargo", "id_oficina", "updated_at"] },
  { name: "administrados_direcciones", id: "id_direccion", fields: ["id_administrado", "tipo_inmueble", "calle", "numero", "comuna_distrito", "ciudad", "updated_at"] },
  { name: "expedientes_generales", id: "id_expediente", fields: ["nro_expediente_anual", "fecha_hora_apertura", "asunto_general", "estado_global", "updated_at"] },
  { name: "documentos_ingresados", id: "id_documento", fields: ["nro_documento_unico", "id_expediente", "id_tipo_documento", "id_administrado", "cantidad_folios", "fecha_hora_recepcion", "ruta_foto", "updated_at"] },
  { name: "hojas_ruta_derivaciones", id: "id_derivacion", fields: ["codigo_barras_seguimiento", "id_documento", "id_empleado_asignado", "id_oficina_procedencia", "fecha_hora_despacho", "prioridad_envio", "fecha_hora_recepcion", "observaciones_receptor", "estado_derivacion", "latitud", "longitud", "updated_at"] },
  { name: "archivo_fisico_central", id: "id_ubicacion", fields: ["codigo_almacen", "nro_pabellon", "nro_estante", "nro_caja_fisica", "updated_at"] },
  { name: "actas_archivamiento", id: "id_acta", fields: ["nro_acta_unico", "id_derivacion", "id_ubicacion_archivo", "fecha_hora_guardado", "costo_digitalizacion", "costo_arancel_custodia", "costo_final_procesamiento", "updated_at"] }
];
const SYNC_FIELDS = ["remote_uuid", "deleted", "version"];
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const RELATIONS = {
  personal_especialistas: { id_oficina: ["oficinas", "id_oficina"] },
  administrados_direcciones: { id_administrado: ["administrados", "id_administrado"] },
  documentos_ingresados: { id_expediente: ["expedientes_generales", "id_expediente"], id_tipo_documento: ["tipos_documentos", "id_tipo_documento"], id_administrado: ["administrados", "id_administrado"] },
  hojas_ruta_derivaciones: { id_documento: ["documentos_ingresados", "id_documento"], id_empleado_asignado: ["personal_especialistas", "id_empleado"], id_oficina_procedencia: ["oficinas", "id_oficina"] },
  actas_archivamiento: { id_derivacion: ["hojas_ruta_derivaciones", "id_derivacion"], id_ubicacion_archivo: ["archivo_fisico_central", "id_ubicacion"] }
};

const relationFields = tableName => Object.keys(RELATIONS[tableName] || {}).map(field => `${field}_remote_uuid`);

function normalizeValue(field, value) {
  if (field.startsWith("fecha_hora") && typeof value === "number") {
    return new Date(value);
  }
  return value;
}

function updatedAt(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

function validationError(message) {
  const error = new Error(message);
  error.status = 400;
  return error;
}

function authorizationError(message) {
  const error = new Error(message);
  error.status = 403;
  return error;
}

function validateRecord(item) {
  if (!item || typeof item !== "object" || !item.datos || typeof item.datos !== "object" || Array.isArray(item.datos)) {
    throw validationError("Registro de sincronizacion invalido");
  }
  const table = tables.find(entry => entry.name === item.tabla);
  if (!table) {
    throw validationError(`Tabla no permitida: ${String(item.tabla || "")}`);
  }
  const allowed = new Set([table.id, ...table.fields, ...SYNC_FIELDS, ...relationFields(table.name)]);
  const unknown = Object.keys(item.datos).filter(field => !allowed.has(field));
  if (unknown.length > 0) {
    throw validationError(`Campos no permitidos en ${table.name}: ${unknown.join(", ")}`);
  }
  if (!UUID_PATTERN.test(String(item.datos.remote_uuid || ""))) {
    throw validationError(`remote_uuid invalido en ${table.name}`);
  }
  const fields = [...table.fields, ...SYNC_FIELDS].filter(field => item.datos[field] !== undefined);
  if (fields.length === 0) {
    throw validationError(`No hay datos para sincronizar en ${table.name}`);
  }
  return { table, fields };
}

router.get("/sincronizacion", async (req, res) => {
  try {
    const desde = Number(req.query.desde || 0);
    if (!Number.isFinite(desde) || desde < 0) {
      return res.status(400).json({ error: "Parametro desde invalido" });
    }
    const data = {};
    for (const table of tables) {
      if (!canRead(req.user && req.user.rol, table.name)) continue;
      const filters = ["updated_at >= $1"];
      const params = [desde];
      if (table.name === "hojas_ruta_derivaciones" && req.user.rol === "ESPECIALISTA") {
        params.push(req.user.id_empleado || -1);
        filters.push(`id_empleado_asignado = $${params.length}`);
      }
      if (table.name === "hojas_ruta_derivaciones" && req.user.rol === "ARCHIVO") {
        params.push(req.user.id_oficina || -1);
        filters.push(`id_oficina_procedencia = $${params.length}`);
      }
      const result = await pool.query(
        `select * from ${table.name} where ${filters.join(" and ")} order by ${table.id} asc`, params
      );
      data[table.name] = result.rows;
    }
    res.json({ data, timestamp: Date.now() });
  } catch (error) {
    console.error("Error al obtener sincronizacion", error);
    res.status(500).json({ error: "Error al obtener sincronizacion" });
  }
});

router.post("/sync-data", async (req, res) => {
  const registros = Array.isArray(req.body) ? req.body : req.body && req.body.registros;
  if (!Array.isArray(registros)) {
    return res.status(400).json({ error: "El cuerpo debe contener una lista de registros" });
  }
  if (registros.length > MAX_BATCH_SIZE) {
    return res.status(413).json({ error: `El lote supera el maximo de ${MAX_BATCH_SIZE} registros` });
  }

  let client;
  try {
    client = await pool.connect();
    await client.query("begin");
    const procesados = [];

    for (const item of registros) {
      const { table, fields } = validateRecord(item);
      const local = item.datos;
      const remoteUuid = local.remote_uuid;
      let result;
      if (!canWrite(req.user && req.user.rol, table.name)) {
        throw authorizationError(`El rol no puede modificar ${table.name}`);
      }
      let resolucion = "INSERTADO";
      for (const [foreignKey, target] of Object.entries(RELATIONS[table.name] || {})) {
        const uuidField = `${foreignKey}_remote_uuid`;
        if (local[uuidField] === undefined || local[uuidField] === null) continue;
        if (!UUID_PATTERN.test(String(local[uuidField]))) {
          throw validationError(`${uuidField} invalido en ${table.name}`);
        }
        const reference = await client.query(
          `select ${target[1]} as id from ${target[0]} where remote_uuid = $1 and deleted = false`,
          [local[uuidField]]
        );
        if (!reference.rows[0]) {
          const error = validationError(`Relacion no sincronizada: ${uuidField}`);
          error.status = 409;
          throw error;
        }
        local[foreignKey] = reference.rows[0].id;
        if (!fields.includes(foreignKey)) fields.push(foreignKey);

      }
      const current = await client.query(
        `select * from ${table.name} where remote_uuid = $1 for update`, [remoteUuid]
      );
      const server = current.rows[0];
      const localVersion = Number(local.version || 0);
      if (server && Number(server.version || 0) > localVersion
          && updatedAt(server.updated_at) > updatedAt(local.updated_at)) {
        procesados.push({
          tabla: table.name,
          remote_uuid: remoteUuid,
          datos: server,
          resolucion: "CONFLICTO_SERVIDOR_GANA"
        });
        continue;
      }

      local.version = Math.max(localVersion, Number(server && server.version || 0)) + 1;
      const values = fields.map(field => normalizeValue(field, local[field]));
      if (server) {
        values.push(remoteUuid);
        const setters = fields.map((field, index) => `${field} = $${index + 1}`).join(", ");
        result = await client.query(
          `update ${table.name} set ${setters} where remote_uuid = $${fields.length + 1} returning *`, values
        );
        resolucion = "CLIENTE_GANA";
      } else {
        const params = fields.map((field, index) => `$${index + 1}`).join(", ");
        result = await client.query(
          `insert into ${table.name} (${fields.join(", ")}) values (${params}) returning *`, values
        );
      }

      procesados.push({ tabla: table.name, datos: result.rows[0], resolucion });
    }

    await client.query("commit");
    res.json({ procesados, timestamp: Date.now() });
  } catch (error) {
    if (client) {
      await client.query("rollback").catch(() => {});
    }
    const status = error.status || 500;
    if (status >= 500) {
      console.error("Error al sincronizar datos", error);
    }
    res.status(status).json({ error: status === 500 ? "Error al sincronizar datos" : error.message });
  } finally {
    if (client) {
      client.release();
    }
  }
});

module.exports = router;
