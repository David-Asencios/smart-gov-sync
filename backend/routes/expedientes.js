const crud = require("./crudFactory");
module.exports = crud("expedientes_generales", "id_expediente", ["nro_expediente_anual", "fecha_hora_apertura", "asunto_general", "estado_global", "updated_at"]);
