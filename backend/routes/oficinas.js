const crud = require("./crudFactory");
module.exports = crud("oficinas", "id_oficina", ["codigo_oficina", "siglas_oficiales", "nombre_unidad", "updated_at"]);
