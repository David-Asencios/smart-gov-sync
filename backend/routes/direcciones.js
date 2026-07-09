const crud = require("./crudFactory");
module.exports = crud("administrados_direcciones", "id_direccion", ["id_administrado", "tipo_inmueble", "calle", "numero", "comuna_distrito", "ciudad", "updated_at"]);
