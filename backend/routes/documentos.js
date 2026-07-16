const crud = require("./crudFactory");
const pool = require("../db");

const router = crud("documentos_ingresados", "id_documento", ["nro_documento_unico", "id_expediente", "id_tipo_documento", "id_administrado", "cantidad_folios", "fecha_hora_recepcion", "ruta_foto", "ruta_adjunto", "nombre_adjunto", "tipo_mime_adjunto", "updated_at"]);

router.get("/:id/adjunto", async (req, res) => {
  try {
    const params = [req.params.id];
    let access = "true";
    if (req.user.rol === "MESA_PARTES") {
      params.push(req.user.id_usuario || -1);
      access = `e.id_usuario_registro = $${params.length}`;
    } else if (req.user.rol === "ESPECIALISTA") {
      params.push(req.user.id_empleado || -1);
      access = `exists (select 1 from hojas_ruta_derivaciones h
        where h.id_documento = d.id_documento and h.id_empleado_asignado = $${params.length} and h.deleted = false)`;
    } else if (req.user.rol === "ARCHIVO") {
      access = `exists (select 1 from hojas_ruta_derivaciones h
        where h.id_documento = d.id_documento and h.estado_derivacion in ('FINALIZADO','ARCHIVADO') and h.deleted = false)`;
    }
    const result = await pool.query(`select d.ruta_adjunto, d.nombre_adjunto, d.tipo_mime_adjunto
      from documentos_ingresados d join expedientes_generales e on e.id_expediente = d.id_expediente
      where d.id_documento = $1 and d.deleted = false and ${access}`, params);
    const item = result.rows[0];
    if (!item) return res.status(404).json({ error: "Adjunto no encontrado" });
    const match = String(item.ruta_adjunto || "").match(/^data:(application\/pdf|image\/jpeg|image\/png);base64,(.+)$/s);
    if (!match) return res.status(404).json({ error: "El documento no tiene archivo adjunto" });
    const safeName = String(item.nombre_adjunto || "documento").replace(/[\r\n"\\]/g, "_");
    res.setHeader("Content-Type", item.tipo_mime_adjunto || match[1]);
    res.setHeader("Content-Disposition", `inline; filename="${safeName}"`);
    res.setHeader("Cache-Control", "private, no-store");
    res.send(Buffer.from(match[2], "base64"));
  } catch (error) {
    console.error("Error al descargar adjunto", error);
    res.status(500).json({ error: "No se pudo descargar el adjunto" });
  }
});

module.exports = router;
