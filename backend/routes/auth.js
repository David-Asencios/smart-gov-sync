const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const pool = require("../db");
const { jwtSecret } = require("../config");
const { normalizeRole } = require("../access-control");
const loginLimiter = require("../middleware/loginLimiter");

const router = express.Router();

router.post("/login", loginLimiter, async (req, res) => {
  try {
    const username = String(req.body.username || "").trim();
    const password = String(req.body.password || "");
    if (!username || !password) {
      return res.status(400).json({ error: "Credenciales requeridas" });
    }

    const result = await pool.query(`
      select u.id_usuario, u.username, u.password_hash, u.rol, u.activo,
             p.id_empleado, p.nombre_completo, p.id_oficina,
             o.nombre_unidad as nombre_oficina
        from usuario u
        left join personal_especialistas p on p.id_empleado = u.id_empleado
        left join oficinas o on o.id_oficina = p.id_oficina
       where lower(u.username) = lower($1)
       limit 1
    `, [username]);
    const user = result.rows[0];
    const role = user && normalizeRole(user.rol);
    if (!user || !user.activo || !role || !(await bcrypt.compare(password, user.password_hash))) {
      return res.status(401).json({ error: "Credenciales invalidas o usuario inactivo" });
    }

    const claims = {
      id_usuario: user.id_usuario,
      username: user.username,
      rol: role,
      id_empleado: user.id_empleado || null,
      id_oficina: user.id_oficina || null
    };
    const token = jwt.sign(claims, jwtSecret, { expiresIn: "8h" });
    res.json({
      token,
      ...claims,
      nombre_completo: user.nombre_completo || user.username,
      nombre_oficina: user.nombre_oficina || "Sin oficina asignada"
    });
  } catch (error) {
    console.error("Error de autenticacion", error);
    res.status(500).json({ error: "Error interno de autenticacion" });
  }
});

module.exports = router;
