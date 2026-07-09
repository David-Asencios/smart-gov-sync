const crud = require("./crudFactory");
module.exports = crud("actas_archivamiento", "id_acta", ["nro_acta_unico", "id_derivacion", "id_ubicacion_archivo", "fecha_hora_guardado", "costo_digitalizacion", "costo_arancel_custodia", "costo_final_procesamiento", "updated_at"]);
