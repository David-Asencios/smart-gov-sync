const express = require("express");
const bcrypt = require("bcryptjs");
const pool = require("../db");
const { normalizeRole } = require("../access-control");

const router = express.Router();
const publicColumns = `u.id_usuario, u.username, u.rol, u.activo, u.id_empleado,
  u.updated_at, p.nombre_completo, p.id_oficina, p.remote_uuid as id_empleado_remote_uuid`;

router.use((req, res, next) => {
  if (!req.user || req.user.rol !== "ADMIN") {
    return res.status(403).json({ error: "Solo un administrador puede gestionar usuarios" });
  }
  next();
});

function cleanBody(body, partial = false) {
  const allowed = new Set(["username", "password", "rol", "activo", "id_empleado_remote_uuid"]);
  const unknown = Object.keys(body || {}).filter(field => !allowed.has(field));
  if (unknown.length) return { error: `Campos no permitidos: ${unknown.join(", ")}` };
  const data = {};
  if (body.username !== undefined) data.username = String(body.username).trim();
  if (body.password !== undefined) data.password = String(body.password);
  if (body.rol !== undefined) data.rol = normalizeRole(body.rol);
  if (body.activo !== undefined) data.activo = body.activo === true;
  if (body.id_empleado_remote_uuid !== undefined) data.id_empleado_remote_uuid = String(body.id_empleado_remote_uuid || "").trim();
  if ((!partial || body.username !== undefined) && !data.username) return { error: "El usuario es obligatorio" };
  if ((!partial || body.rol !== undefined) && !data.rol) return { error: "El rol no es valido" };
  if ((!partial || body.id_empleado_remote_uuid !== undefined) && !data.id_empleado_remote_uuid) return { error: "El empleado no es valido" };
  if ((!partial || body.password !== undefined) && (!data.password || data.password.length < 8)) return { error: "La contrasena debe tener al menos 8 caracteres" };
  return { data };
}

function databaseError(res, error) {
  if (error.code === "23505") return res.status(409).json({ error: "El nombre de usuario ya existe" });
  if (error.code === "23503") return res.status(409).json({ error: "El empleado no existe" });
  console.error("Error al gestionar usuarios", error);
  return res.status(500).json({ error: "Error al gestionar usuarios" });
}

async function findUser(id) {
  return pool.query(`select ${publicColumns} from usuario u left join personal_especialistas p
    on p.id_empleado = u.id_empleado where u.id_usuario = $1`, [id]);
}

async function employeeId(client, remoteUuid) {
  const result = await client.query("select id_empleado from personal_especialistas where remote_uuid = $1 and deleted = false", [remoteUuid]);
  return result.rows[0] && result.rows[0].id_empleado;
}

router.get("/", async (req, res) => {
  try {
    const result = await pool.query(`select ${publicColumns} from usuario u left join personal_especialistas p
      on p.id_empleado = u.id_empleado order by u.id_usuario desc`);
    res.json(result.rows);
  } catch (error) { databaseError(res, error); }
});

router.post("/", async (req, res) => {
  const parsed = cleanBody(req.body || {});
  if (parsed.error) return res.status(400).json({ error: parsed.error });
  try {
    const data = parsed.data;
    const passwordHash = await bcrypt.hash(data.password, 12);
    const idEmpleado = await employeeId(pool, data.id_empleado_remote_uuid);
    if (!idEmpleado) return res.status(409).json({ error: "El empleado no existe o esta inactivo" });
    const inserted = await pool.query(`insert into usuario
      (username, password_hash, rol, activo, id_empleado, updated_at)
      values ($1, $2, $3, $4, $5, $6) returning id_usuario`,
    [data.username, passwordHash, data.rol, data.activo === undefined ? true : data.activo, idEmpleado, Date.now()]);
    const result = await findUser(inserted.rows[0].id_usuario);
    res.status(201).json(result.rows[0]);
  } catch (error) { databaseError(res, error); }
});

router.put("/:id", async (req, res) => {
  const parsed = cleanBody(req.body || {}, true);
  if (parsed.error) return res.status(400).json({ error: parsed.error });
  try {
    const data = parsed.data;
    if (Number(req.params.id) === Number(req.user.id_usuario)
        && (data.activo === false || (data.rol !== undefined && data.rol !== "ADMIN"))) {
      return res.status(409).json({ error: "No puede desactivar ni quitar el rol a su propia cuenta" });
    }
    const target = await pool.query("select rol, activo from usuario where id_usuario = $1", [req.params.id]);
    if (!target.rows[0]) return res.status(404).json({ error: "Usuario no encontrado" });
    if (target.rows[0].rol === "ADMIN" && target.rows[0].activo
        && (data.activo === false || (data.rol !== undefined && data.rol !== "ADMIN"))) {
      const admins = await pool.query("select count(*)::int as total from usuario where rol = 'ADMIN' and activo = true");
      if (admins.rows[0].total <= 1) return res.status(409).json({ error: "Debe existir al menos un administrador activo" });
    }
    const fields = [];
    const values = [];
    for (const field of ["username", "rol", "activo"]) {
      if (data[field] !== undefined) { values.push(data[field]); fields.push(`${field} = $${values.length}`); }
    }
    if (data.id_empleado_remote_uuid !== undefined) {
      const idEmpleado = await employeeId(pool, data.id_empleado_remote_uuid);
      if (!idEmpleado) return res.status(409).json({ error: "El empleado no existe o esta inactivo" });
      values.push(idEmpleado); fields.push(`id_empleado = $${values.length}`);
    }
    if (data.password !== undefined) {
      values.push(await bcrypt.hash(data.password, 12));
      fields.push(`password_hash = $${values.length}`);
    }
    if (!fields.length) return res.status(400).json({ error: "No hay datos para actualizar" });
    values.push(Date.now()); fields.push(`updated_at = $${values.length}`);
    values.push(req.params.id);
    const updated = await pool.query(`update usuario set ${fields.join(", ")} where id_usuario = $${values.length} returning id_usuario`, values);
    if (!updated.rows[0]) return res.status(404).json({ error: "Usuario no encontrado" });
    const result = await findUser(req.params.id);
    res.json(result.rows[0]);
  } catch (error) { databaseError(res, error); }
});

module.exports = router;
module.exports.cleanBody = cleanBody;
