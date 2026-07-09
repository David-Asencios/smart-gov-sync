const crud = require("./crudFactory");
module.exports = crud("personal_especialistas", "id_empleado", ["codigo_empleado", "nombre_completo", "cargo", "id_oficina", "updated_at"]);
