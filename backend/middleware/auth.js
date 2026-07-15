const jwt = require("jsonwebtoken");
const pool = require("../db");
const { jwtSecret } = require("../config");
const { normalizeRole } = require("../access-control");

module.exports = async function auth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: "Token requerido" });

  try {
    const claims = jwt.verify(token, jwtSecret);
    const result = await pool.query(`
      select u.id_usuario, u.username, u.rol, u.activo,
             p.id_empleado, p.id_oficina
        from usuario u
        left join personal_especialistas p on p.id_empleado = u.id_empleado
       where u.id_usuario = $1
       limit 1
    `, [claims.id_usuario]);
    const user = result.rows[0];
    const role = user && normalizeRole(user.rol);
    if (!user || !user.activo || !role) {
      return res.status(401).json({ error: "Sesion no valida o usuario inactivo" });
    }
    req.user = {
      id_usuario: user.id_usuario,
      username: user.username,
      rol: role,
      id_empleado: user.id_empleado || null,
      id_oficina: user.id_oficina || null
    };
    next();
  } catch (error) {
    return res.status(401).json({ error: "Token invalido o vencido" });
  }
};
