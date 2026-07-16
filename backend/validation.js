const ALLOWED_EXPEDIENTE_ESTADOS = new Set(["ABIERTO", "EN_PROCESO", "CERRADO", "ARCHIVADO"]);
const ALLOWED_DERIVACION_ESTADOS = new Set(["PENDIENTE", "RECIBIDO", "RECHAZADO", "FINALIZADO", "ARCHIVADO"]);
const ALLOWED_PRIORIDADES = new Set(["BAJA", "NORMAL", "ALTA"]);

const REQUIRED_BY_TABLE = {
  oficinas: ["codigo_oficina", "nombre_unidad"],
  tipos_documentos: ["nombre_tipo_documento"],
  administrados: ["dni_ruc", "nombre_razon_social"],
  personal_especialistas: ["nombre_completo", "id_oficina"],
  administrados_direcciones: ["id_administrado"],
  expedientes_generales: ["nro_expediente_anual", "asunto_general", "estado_global"],
  documentos_ingresados: ["nro_documento_unico", "id_expediente", "id_tipo_documento", "id_administrado"],
  hojas_ruta_derivaciones: ["codigo_barras_seguimiento", "id_documento", "id_empleado_asignado", "id_oficina_procedencia", "prioridad_envio", "estado_derivacion"],
  archivo_fisico_central: ["codigo_almacen", "nro_pabellon", "nro_estante", "nro_caja_fisica"],
  actas_archivamiento: ["nro_acta_unico", "id_derivacion", "id_ubicacion_archivo"]
};

function isBlank(value) {
  return value === undefined || value === null || String(value).trim() === "";
}

function numberOrNull(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function requireFields(table, data, partial = false) {
  if (partial) return;
  const missing = (REQUIRED_BY_TABLE[table] || []).filter(field =>
    isBlank(data[field]) && isBlank(data[`${field}_remote_uuid`])
  );
  if (missing.length) {
    return `Campos obligatorios faltantes: ${missing.join(", ")}`;
  }
}

function validate(table, data, options = {}) {
  const requiredError = requireFields(table, data || {}, Boolean(options.partial));
  if (requiredError) return requiredError;

  if (data.estado_global !== undefined
      && !ALLOWED_EXPEDIENTE_ESTADOS.has(String(data.estado_global).trim().toUpperCase())) {
    return "Estado de expediente invalido";
  }
  if (data.estado_derivacion !== undefined
      && !ALLOWED_DERIVACION_ESTADOS.has(String(data.estado_derivacion).trim().toUpperCase())) {
    return "Estado de derivacion invalido";
  }
  if (data.prioridad_envio !== undefined
      && !ALLOWED_PRIORIDADES.has(String(data.prioridad_envio).trim().toUpperCase())) {
    return "Prioridad de envio invalida";
  }
  if (data.cantidad_folios !== undefined) {
    const folios = numberOrNull(data.cantidad_folios);
    if (folios === null || folios < 1) return "La cantidad de folios debe ser mayor a cero";
  }
  if (data.ruta_foto !== undefined && data.ruta_foto !== null) {
    const evidence = String(data.ruta_foto);
    if (!evidence.startsWith("data:image/jpeg;base64,")) return "La evidencia fotografica no tiene un formato valido";
    if (evidence.length > 2_000_000) return "La evidencia fotografica supera el tamano permitido";
  }
  for (const field of ["id_oficina", "id_administrado", "id_expediente", "id_tipo_documento", "id_documento", "id_empleado_asignado", "id_oficina_procedencia", "id_derivacion", "id_ubicacion_archivo"]) {
    if (data[field] !== undefined) {
      const id = numberOrNull(data[field]);
      if (id === null || id < 1) return `${field} debe ser un identificador valido`;
    }
  }
  for (const field of ["nro_pabellon", "nro_estante", "nro_caja_fisica"]) {
    if (data[field] !== undefined) {
      const value = numberOrNull(data[field]);
      if (value === null || value < 1) return `${field} debe ser mayor a cero`;
    }
  }
  for (const field of ["latitud", "longitud"]) {
    if (data[field] !== undefined && data[field] !== null) {
      const value = numberOrNull(data[field]);
      if (value === null) return `${field} debe ser numerico`;
      if (field === "latitud" && (value < -90 || value > 90)) return "Latitud fuera de rango";
      if (field === "longitud" && (value < -180 || value > 180)) return "Longitud fuera de rango";
    }
  }
  for (const field of ["costo_digitalizacion", "costo_arancel_custodia", "costo_final_procesamiento"]) {
    if (data[field] !== undefined) {
      const value = numberOrNull(data[field]);
      if (value === null || value < 0) return `${field} no puede ser negativo`;
    }
  }
  if (data.costo_final_procesamiento !== undefined
      && data.costo_digitalizacion !== undefined && data.costo_arancel_custodia !== undefined) {
    const expected = Number(data.costo_digitalizacion) + Number(data.costo_arancel_custodia);
    if (Math.abs(Number(data.costo_final_procesamiento) - expected) > 0.01) return "El costo final no coincide con sus componentes";
  }
  return null;
}

function normalizeEnums(data) {
  for (const field of ["estado_global", "estado_derivacion", "prioridad_envio"]) {
    if (data[field] !== undefined && data[field] !== null) {
      data[field] = String(data[field]).trim().toUpperCase();
    }
  }
  return data;
}

module.exports = { validate, normalizeEnums };
