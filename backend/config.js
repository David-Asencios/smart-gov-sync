const jwtSecret = String(process.env.JWT_SECRET || "").trim();

if (!jwtSecret || jwtSecret.length < 32) {
  throw new Error("JWT_SECRET es obligatorio y debe tener al menos 32 caracteres");
}

module.exports = {
  jwtSecret,
  allowedRoles: new Set(["ADMIN", "MESA_PARTES", "ESPECIALISTA", "ARCHIVO"])
};
