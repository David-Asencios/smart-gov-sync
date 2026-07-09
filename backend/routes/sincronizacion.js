const express = require("express");
const pool = require("../db");

const router = express.Router();

const tables = [
  { name: "oficinas", id: "id_oficina" },
  { name: "tipos_documentos", id: "id_tipo_documento" },
  { name: "administrados", id: "id_administrado" },
  { name: "personal_especialistas", id: "id_empleado" },
  { name: "administrados_direcciones", id: "id_direccion" },
  { name: "expedientes_generales", id: "id_expediente" },
  { name: "documentos_ingresados", id: "id_documento" },
  { name: "hojas_ruta_derivaciones", id: "id_derivacion" },
  { name: "archivo_fisico_central", id: "id_ubicacion" },
  { name: "actas_archivamiento", id: "id_acta" },
  { name: "usuario", id: "id_usuario" }
];

router.get("/sincronizacion", async (req, res) => {
  const desde = Number(req.query.desde || 0);
  const data = {};
  for (const table of tables) {
    const result = await pool.query(`select * from ${table.name} where updated_at >= $1 order by ${table.id} asc`, [desde]);
    data[table.name] = result.rows;
  }
  res.json({ data, timestamp: Date.now() });
});

router.post("/sync-data", async (req, res) => {
  const registros = Array.isArray(req.body) ? req.body : req.body.registros || [];
  const procesados = [];
  for (const item of registros) {
    const table = tables.find(entry => entry.name === item.tabla);
    if (!table || !item.datos) {
      continue;
    }
    const local = item.datos;
    const idValue = local[table.id];
    if (idValue) {
      const current = await pool.query(`select * from ${table.name} where ${table.id} = $1`, [idValue]);
      const server = current.rows[0];
      if (server && Number(server.updated_at || 0) > Number(local.updated_at || 0)) {
        procesados.push({ tabla: table.name, datos: server });
        continue;
      }
      const fields = Object.keys(local).filter(field => field !== table.id);
      const values = fields.map(field => local[field]);
      values.push(idValue);
      const setters = fields.map((field, index) => `${field} = $${index + 1}`).join(", ");
      const updated = await pool.query(`update ${table.name} set ${setters} where ${table.id} = $${fields.length + 1} returning *`, values);
      procesados.push({ tabla: table.name, datos: updated.rows[0] });
    } else {
      const fields = Object.keys(local).filter(field => field !== table.id);
      const values = fields.map(field => local[field]);
      const params = fields.map((field, index) => `$${index + 1}`).join(", ");
      const inserted = await pool.query(`insert into ${table.name} (${fields.join(", ")}) values (${params}) returning *`, values);
      procesados.push({ tabla: table.name, datos: inserted.rows[0] });
    }
  }
  res.json({ procesados, timestamp: Date.now() });
});

module.exports = router;
