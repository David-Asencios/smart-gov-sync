const crud = require("./crudFactory");
module.exports = crud("tipos_documentos", "id_tipo_documento", ["nombre_tipo_documento", "updated_at"]);
