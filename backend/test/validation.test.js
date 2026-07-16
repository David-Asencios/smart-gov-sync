process.env.JWT_SECRET = process.env.JWT_SECRET || "test_secret_for_smart_gov_sync_2026";
const test = require("node:test");
const assert = require("node:assert/strict");
const { validate } = require("../validation");

test("los costos de archivo no pueden ser negativos", () => {
  assert.match(validate("actas_archivamiento", { costo_digitalizacion: -1 }, { partial: true }), /negativo/);
});

test("el costo final debe coincidir con digitalizacion y custodia", () => {
  assert.match(validate("actas_archivamiento", {
    costo_digitalizacion: 10, costo_arancel_custodia: 5, costo_final_procesamiento: 20
  }, { partial: true }), /no coincide/);
  assert.equal(validate("actas_archivamiento", {
    costo_digitalizacion: 10, costo_arancel_custodia: 5, costo_final_procesamiento: 15
  }, { partial: true }), null);
});
