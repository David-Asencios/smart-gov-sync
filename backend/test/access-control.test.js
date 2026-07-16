process.env.JWT_SECRET = process.env.JWT_SECRET || "test_secret_for_smart_gov_sync_2026";
const test = require("node:test");
const assert = require("node:assert/strict");
const { normalizeRole, canWrite } = require("../access-control");

test("un rol ausente o desconocido nunca obtiene privilegios", () => {
  assert.equal(normalizeRole(null), null);
  assert.equal(normalizeRole("SUPERVISOR"), null);
  assert.equal(canWrite(null, "oficinas"), false);
  assert.equal(canWrite("SUPERVISOR", "expedientes_generales"), false);
});

test("cada rol solo escribe sus tablas", () => {
  assert.equal(canWrite("ADMIN", "oficinas"), true);
  assert.equal(canWrite("ADMIN", "personal_especialistas"), true);
  assert.equal(canWrite("ADMIN", "expedientes_generales"), false);
  assert.equal(canWrite("MESA_PARTES", "documentos_ingresados"), true);
  assert.equal(canWrite("MESA_PARTES", "hojas_ruta_derivaciones"), true);
  assert.equal(canWrite("MESA_PARTES", "archivo_fisico_central"), false);
  assert.equal(canWrite("ESPECIALISTA", "hojas_ruta_derivaciones"), true);
  assert.equal(canWrite("ARCHIVO", "expedientes_generales"), true);
  assert.equal(canWrite("ARCHIVO", "actas_archivamiento"), true);
});
