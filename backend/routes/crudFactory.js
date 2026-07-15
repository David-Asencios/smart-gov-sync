const express = require("express");
const pool = require("../db");
const { canRead, canWrite } = require("../access-control");

function normalizeValue(field, value) {
  if (field.startsWith("fecha_hora") && typeof value === "number") return new Date(value);
  return value;
}

function sendDatabaseError(res, error, fallback) {
  if (error.code === "23505") return res.status(409).json({ error: "El registro ya existe" });
  if (error.code === "23503") return res.status(409).json({ error: "El registro esta relacionado con otros datos" });
  if (error.code === "22P02" || error.code === "23514" || error.code === "23502") {
    return res.status(400).json({ error: "Los datos enviados no son validos" });
  }
  console.error(fallback, error);
  return res.status(500).json({ error: fallback });
}

function scope(table, user, offset = 1) {
  if (table !== "hojas_ruta_derivaciones" || user.rol === "ADMIN") return { sql: "", params: [] };
  if (user.rol === "ESPECIALISTA") {
    return { sql: `id_empleado_asignado = $${offset}`, params: [user.id_empleado || -1] };
  }
  if (user.rol === "ARCHIVO") {
    return { sql: `id_oficina_procedencia = $${offset}`, params: [user.id_oficina || -1] };
  }
  return { sql: "false", params: [] };
}

function createCrudRouter(table, idField, allowedFields) {
  const router = express.Router();
  const writableFields = allowedFields.filter(field => field !== "updated_at");

  router.use((req, res, next) => {
    const permitted = req.method === "GET" ? canRead(req.user && req.user.rol, table) : canWrite(req.user && req.user.rol, table);
    if (!permitted) return res.status(403).json({ error: "No tiene permisos para este recurso" });
    next();
  });

  router.get("/", async (req, res) => {
    try {
      const own = scope(table, req.user);
      const where = ["deleted = false", own.sql].filter(Boolean).join(" and ");
      const result = await pool.query(`select * from ${table} where ${where} order by ${idField} asc`, own.params);
      res.json(result.rows);
    } catch (error) { sendDatabaseError(res, error, "Error al listar registros"); }
  });

  router.get("/:id", async (req, res) => {
    try {
      const own = scope(table, req.user, 2);
      const where = [`${idField} = $1`, "deleted = false", own.sql].filter(Boolean).join(" and ");
      const result = await pool.query(`select * from ${table} where ${where}`, [req.params.id, ...own.params]);
      if (!result.rows[0]) return res.status(404).json({ error: "No encontrado" });
      res.json(result.rows[0]);
    } catch (error) { sendDatabaseError(res, error, "Error al obtener registro"); }
  });

  router.post("/", async (req, res) => {
    try {
      const unknown = Object.keys(req.body || {}).filter(field => !writableFields.includes(field));
      if (unknown.length) return res.status(400).json({ error: `Campos no permitidos: ${unknown.join(", ")}` });
      const fields = writableFields.filter(field => req.body[field] !== undefined);
      if (!fields.length) return res.status(400).json({ error: "Datos requeridos" });
      const insertFields = [...fields, "updated_at", "version"];
      const values = [...fields.map(field => normalizeValue(field, req.body[field])), Date.now(), 1];
      const params = insertFields.map((field, index) => `$${index + 1}`).join(", ");
      const result = await pool.query(`insert into ${table} (${insertFields.join(", ")}) values (${params}) returning *`, values);
      res.status(201).json(result.rows[0]);
    } catch (error) { sendDatabaseError(res, error, "Error al crear registro"); }
  });

  router.put("/:id", async (req, res) => {
    try {
      const unknown = Object.keys(req.body || {}).filter(field => !writableFields.includes(field));
      if (unknown.length) return res.status(400).json({ error: `Campos no permitidos: ${unknown.join(", ")}` });
      const fields = writableFields.filter(field => req.body[field] !== undefined);
      if (!fields.length) return res.status(400).json({ error: "Datos requeridos" });
      const values = fields.map(field => normalizeValue(field, req.body[field]));
      const own = scope(table, req.user, fields.length + 2);
      values.push(req.params.id, ...own.params);
      const setters = fields.map((field, index) => `${field} = $${index + 1}`).join(", ");
      const where = [`${idField} = $${fields.length + 1}`, "deleted = false", own.sql].filter(Boolean).join(" and ");
      const result = await pool.query(
        `update ${table} set ${setters}, updated_at = ${Date.now()}, version = version + 1 where ${where} returning *`, values
      );
      if (!result.rows[0]) return res.status(404).json({ error: "No encontrado" });
      res.json(result.rows[0]);
    } catch (error) { sendDatabaseError(res, error, "Error al actualizar registro"); }
  });

  router.delete("/:id", async (req, res) => {
    try {
      const own = scope(table, req.user, 2);
      const where = [`${idField} = $1`, "deleted = false", own.sql].filter(Boolean).join(" and ");
      const result = await pool.query(
        `update ${table} set deleted = true, updated_at = ${Date.now()}, version = version + 1 where ${where} returning ${idField}`,
        [req.params.id, ...own.params]
      );
      if (!result.rows[0]) return res.status(404).json({ error: "No encontrado" });
      res.json({ eliminado: true });
    } catch (error) { sendDatabaseError(res, error, "Error al eliminar registro"); }
  });

  return router;
}

module.exports = createCrudRouter;
