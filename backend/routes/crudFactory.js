const express = require("express");
const pool = require("../db");

function normalizeValue(field, value) {
  if (field.startsWith("fecha_hora") && typeof value === "number") {
    return new Date(value);
  }
  return value;
}

function createCrudRouter(table, idField, allowedFields) {
  const router = express.Router();

  router.get("/", async (req, res) => {
    try {
      const result = await pool.query(`select * from ${table} order by ${idField} asc`);
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: "Error al listar registros" });
    }
  });

  router.get("/:id", async (req, res) => {
    try {
      const result = await pool.query(`select * from ${table} where ${idField} = $1`, [req.params.id]);
      if (!result.rows[0]) {
        return res.status(404).json({ error: "No encontrado" });
      }
      res.json(result.rows[0]);
    } catch (error) {
      res.status(500).json({ error: "Error al obtener registro" });
    }
  });

  router.post("/", async (req, res) => {
    try {
      const fields = allowedFields.filter(field => req.body[field] !== undefined);
      if (fields.length === 0) {
        return res.status(400).json({ error: "Datos requeridos" });
      }
      const values = fields.map(field => normalizeValue(field, req.body[field]));
      const params = fields.map((field, index) => `$${index + 1}`).join(", ");
      const result = await pool.query(`insert into ${table} (${fields.join(", ")}) values (${params}) returning *`, values);
      res.status(201).json(result.rows[0]);
    } catch (error) {
      res.status(500).json({ error: "Error al crear registro" });
    }
  });

  router.put("/:id", async (req, res) => {
    try {
      const fields = allowedFields.filter(field => req.body[field] !== undefined);
      if (fields.length === 0) {
        return res.status(400).json({ error: "Datos requeridos" });
      }
      const values = fields.map(field => normalizeValue(field, req.body[field]));
      values.push(req.params.id);
      const setters = fields.map((field, index) => `${field} = $${index + 1}`).join(", ");
      const result = await pool.query(`update ${table} set ${setters} where ${idField} = $${fields.length + 1} returning *`, values);
      if (!result.rows[0]) {
        return res.status(404).json({ error: "No encontrado" });
      }
      res.json(result.rows[0]);
    } catch (error) {
      res.status(500).json({ error: "Error al actualizar registro" });
    }
  });

  router.delete("/:id", async (req, res) => {
    try {
      const result = await pool.query(`delete from ${table} where ${idField} = $1 returning *`, [req.params.id]);
      if (!result.rows[0]) {
        return res.status(404).json({ error: "No encontrado" });
      }
      res.json({ eliminado: true });
    } catch (error) {
      res.status(500).json({ error: "Error al eliminar registro" });
    }
  });

  return router;
}

module.exports = createCrudRouter;
