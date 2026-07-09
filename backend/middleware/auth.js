const jwt = require("jsonwebtoken");
const jwtSecret = process.env.JWT_SECRET || "smart_gov_sync_jwt_secret_2026_cambiar_en_render";

module.exports = function auth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ error: "Token requerido" });
  }
  try {
    req.user = jwt.verify(token, jwtSecret);
    next();
  } catch (error) {
    res.status(401).json({ error: "Token invalido" });
  }
};
