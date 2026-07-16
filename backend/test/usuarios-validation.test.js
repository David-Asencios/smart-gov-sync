process.env.JWT_SECRET = process.env.JWT_SECRET || "test_secret_for_smart_gov_sync_2026";
const test = require("node:test");
const assert = require("node:assert/strict");
const { cleanBody } = require("../routes/usuarios");

test("un usuario nuevo requiere credenciales y datos validos", () => {
  assert.match(cleanBody({ username: "ana", password: "corta", rol: "ADMIN", id_empleado_remote_uuid: "uuid" }).error, /8 caracteres/);
  assert.match(cleanBody({ username: "ana", password: "segura123", rol: "OTRO", id_empleado_remote_uuid: "uuid" }).error, /rol/);
  assert.equal(cleanBody({ username: "ana", password: "segura123", rol: "ADMIN", id_empleado_remote_uuid: "" }).error, undefined);
});

test("la actualizacion parcial no exige reenviar la contrasena", () => {
  const result = cleanBody({ activo: false }, true);
  assert.deepEqual(result, { data: { activo: false } });
});

test("un administrador puede registrarse sin empleado", () => {
  assert.equal(cleanBody({ username: "admin2", password: "segura123", rol: "ADMIN", activo: true }).error, undefined);
});

test("no acepta password_hash ni campos desconocidos", () => {
  const result = cleanBody({ username: "ana", password_hash: "texto" }, true);
  assert.match(result.error, /Campos no permitidos/);
});

test("valida el formato y longitud del nombre de usuario", () => {
  assert.match(cleanBody({ username: "a b", password: "segura123", rol: "ADMIN" }).error, /usuario debe tener/);
  assert.match(cleanBody({ username: "ab", password: "segura123", rol: "ADMIN" }).error, /usuario debe tener/);
  assert.equal(cleanBody({ username: "ana.perez_2", password: "segura123", rol: "ADMIN" }).error, undefined);
});

test("limita la contrasena al maximo admitido por bcrypt", () => {
  const result = cleanBody({ username: "ana", password: "x".repeat(73), rol: "ADMIN" });
  assert.match(result.error, /72 caracteres/);
});
