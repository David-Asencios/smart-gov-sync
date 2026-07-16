process.env.JWT_SECRET = process.env.JWT_SECRET || "test_secret_for_smart_gov_sync_2026";
const test = require("node:test");
const assert = require("node:assert/strict");
const { cleanBody } = require("../routes/usuarios");

test("un usuario nuevo requiere credenciales y datos validos", () => {
  assert.match(cleanBody({ username: "ana", password: "corta", rol: "ADMIN", id_empleado: 1 }).error, /8 caracteres/);
  assert.match(cleanBody({ username: "ana", password: "segura123", rol: "OTRO", id_empleado: 1 }).error, /rol/);
  assert.match(cleanBody({ username: "ana", password: "segura123", rol: "ADMIN", id_empleado: 0 }).error, /empleado/);
});

test("la actualizacion parcial no exige reenviar la contrasena", () => {
  const result = cleanBody({ activo: false }, true);
  assert.deepEqual(result, { data: { activo: false } });
});

test("no acepta password_hash ni campos desconocidos", () => {
  const result = cleanBody({ username: "ana", password_hash: "texto" }, true);
  assert.match(result.error, /Campos no permitidos/);
});
