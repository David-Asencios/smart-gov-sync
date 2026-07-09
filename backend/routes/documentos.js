const crud = require("./crudFactory");
module.exports = crud("documentos_ingresados", "id_documento", ["nro_documento_unico", "id_expediente", "id_tipo_documento", "id_administrado", "cantidad_folios", "fecha_hora_recepcion", "ruta_foto", "updated_at"]);
