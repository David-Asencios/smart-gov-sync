const { allowedRoles } = require("./config");

const writableByRole = Object.freeze({
  ADMIN: new Set(["oficinas", "tipos_documentos", "usuario"]),
  MESA_PARTES: new Set(["administrados", "administrados_direcciones", "expedientes_generales", "documentos_ingresados", "hojas_ruta_derivaciones"]),
  ESPECIALISTA: new Set(["hojas_ruta_derivaciones"]),
  ARCHIVO: new Set(["expedientes_generales", "hojas_ruta_derivaciones", "archivo_fisico_central", "actas_archivamiento"])
});

const readableByRole = Object.freeze({
  ADMIN: new Set(["*"]),
  MESA_PARTES: new Set(["oficinas", "tipos_documentos", "administrados", "administrados_direcciones", "expedientes_generales", "documentos_ingresados"]),
  ESPECIALISTA: new Set(["oficinas", "tipos_documentos", "personal_especialistas", "expedientes_generales", "documentos_ingresados", "hojas_ruta_derivaciones"]),
  ARCHIVO: new Set(["oficinas", "expedientes_generales", "documentos_ingresados", "hojas_ruta_derivaciones", "archivo_fisico_central", "actas_archivamiento"])
});

function normalizeRole(role) {
  const value = String(role || "").trim().toUpperCase();
  return allowedRoles.has(value) ? value : null;
}

function allowed(mapping, role, tableName) {
  const normalized = normalizeRole(role);
  if (!normalized) return false;
  const permissions = mapping[normalized];
  return permissions.has("*") || permissions.has(tableName);
}

function canRead(role, tableName) { return allowed(readableByRole, role, tableName); }
function canWrite(role, tableName) { return allowed(writableByRole, role, tableName); }

function requireTableWrite(req, res, next) {
  if (!canWrite(req.user && req.user.rol, req.params.tableName)) {
    return res.status(403).json({ error: "No tiene permisos para realizar esta accion" });
  }
  next();
}

module.exports = { normalizeRole, canRead, canWrite, requireTableWrite, writableByRole, readableByRole };
