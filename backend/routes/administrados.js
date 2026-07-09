const crud = require("./crudFactory");
module.exports = crud("administrados", "id_administrado", ["codigo_administrado", "dni_ruc", "nombre_razon_social", "telefono", "correo_notificaciones", "updated_at"]);
