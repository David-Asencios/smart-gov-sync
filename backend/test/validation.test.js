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

test("la evidencia fotografica debe ser JPEG codificado", () => {
  assert.match(validate("documentos_ingresados", { ruta_foto: "/ruta/privada.jpg" }, { partial: true }), /formato valido/);
  assert.equal(validate("documentos_ingresados", { ruta_foto: "data:image/jpeg;base64,YWJj" }, { partial: true }), null);
});

test("el adjunto digital acepta solo PDF, JPG o PNG coherentes", () => {
  assert.equal(validate("documentos_ingresados", {
    ruta_adjunto: "data:application/pdf;base64,YWJj",
    nombre_adjunto: "solicitud.pdf",
    tipo_mime_adjunto: "application/pdf"
  }, { partial: true }), null);
  assert.match(validate("documentos_ingresados", {
    ruta_adjunto: "data:text/plain;base64,YWJj",
    nombre_adjunto: "nota.txt",
    tipo_mime_adjunto: "text/plain"
  }, { partial: true }), /PDF, JPG o PNG/);
});

test("el numero de expediente admite el codigo generado por Android", () => {
  assert.equal(validate("expedientes_generales", {
    nro_expediente_anual: "EXP-20260716-111338-5666"
  }, { partial: true }), null);
  assert.match(validate("expedientes_generales", {
    nro_expediente_anual: "X".repeat(51)
  }, { partial: true }), /50 caracteres/);
});
