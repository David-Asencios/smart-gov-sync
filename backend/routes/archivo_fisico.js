const crud = require("./crudFactory");
module.exports = crud("archivo_fisico_central", "id_ubicacion", ["codigo_almacen", "nro_pabellon", "nro_estante", "nro_caja_fisica", "updated_at"]);
