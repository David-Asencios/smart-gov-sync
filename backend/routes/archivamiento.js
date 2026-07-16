const express = require("express");
const pool = require("../db");

const router = express.Router();

router.post("/", async (req, res) => {
  if (!req.user || req.user.rol !== "ARCHIVO") return res.status(403).json({ error: "Solo Archivo puede archivar expedientes" });
  const data = req.body || {};
  const integers = [data.nro_pabellon, data.nro_estante, data.nro_caja_fisica].map(Number);
  const digitalizacion = Number(data.costo_digitalizacion);
  const custodia = Number(data.costo_arancel_custodia);
  if (!data.id_derivacion_remote_uuid || !String(data.codigo_almacen || "").trim()
      || integers.some(value => !Number.isInteger(value) || value < 1)
      || !Number.isFinite(digitalizacion) || digitalizacion < 0
      || !Number.isFinite(custodia) || custodia < 0) {
    return res.status(400).json({ error: "Los datos de archivamiento no son validos" });
  }
  const client = await pool.connect();
  try {
    await client.query("begin");
    const route = (await client.query(`select h.*, d.id_expediente from hojas_ruta_derivaciones h
      join documentos_ingresados d on d.id_documento = h.id_documento
      where h.remote_uuid = $1 and h.deleted = false for update`, [data.id_derivacion_remote_uuid])).rows[0];
    if (!route) { await client.query("rollback"); return res.status(404).json({ error: "Derivacion no encontrada" }); }
    if (route.estado_derivacion !== "FINALIZADO") { await client.query("rollback"); return res.status(409).json({ error: "El expediente no esta finalizado" }); }

    const duplicateAct = await client.query("select 1 from actas_archivamiento where id_derivacion = $1 and deleted = false", [route.id_derivacion]);
    if (duplicateAct.rows[0]) { await client.query("rollback"); return res.status(409).json({ error: "La derivacion ya tiene un acta" }); }
    const duplicateCode = await client.query("select 1 from archivo_fisico_central where lower(codigo_almacen) = lower($1) and deleted = false", [String(data.codigo_almacen).trim()]);
    if (duplicateCode.rows[0]) { await client.query("rollback"); return res.status(409).json({ error: "El codigo de archivo ya existe" }); }

    const now = Date.now();
    const location = (await client.query(`insert into archivo_fisico_central
      (codigo_almacen,nro_pabellon,nro_estante,nro_caja_fisica,updated_at,remote_uuid,deleted,version)
      values ($1,$2,$3,$4,$5,gen_random_uuid(),false,1) returning *`,
      [String(data.codigo_almacen).trim(), ...integers, now])).rows[0];
    const actNumber = `ACT-${now}-${Math.random().toString(36).slice(2, 6).toUpperCase()}`;
    const act = (await client.query(`insert into actas_archivamiento
      (nro_acta_unico,id_derivacion,id_ubicacion_archivo,fecha_hora_guardado,costo_digitalizacion,
       costo_arancel_custodia,costo_final_procesamiento,id_usuario_archivo,updated_at,remote_uuid,deleted,version)
      values ($1,$2,$3,now(),$4,$5,$6,$7,$8,gen_random_uuid(),false,1) returning *`,
      [actNumber, route.id_derivacion, location.id_ubicacion, digitalizacion, custodia,
        digitalizacion + custodia, req.user.id_usuario, now])).rows[0];
    const updatedRoute = (await client.query(`update hojas_ruta_derivaciones set estado_derivacion='ARCHIVADO',
      observaciones_receptor='Archivamiento fisico registrado',updated_at=$1,version=version+1
      where id_derivacion=$2 returning *`, [now, route.id_derivacion])).rows[0];
    const expediente = (await client.query(`update expedientes_generales set estado_global='ARCHIVADO',
      updated_at=$1,version=version+1 where id_expediente=$2 returning *`, [now, route.id_expediente])).rows[0];
    await client.query("commit");
    res.status(201).json({ ubicacion: location, acta: act, derivacion: updatedRoute, expediente });
  } catch (error) {
    await client.query("rollback").catch(() => {});
    if (error.code === "23505") return res.status(409).json({ error: "El codigo o acta ya existe" });
    console.error("Error en archivamiento atomico", error);
    res.status(500).json({ error: "No se pudo completar el archivamiento" });
  } finally { client.release(); }
});

module.exports = router;
