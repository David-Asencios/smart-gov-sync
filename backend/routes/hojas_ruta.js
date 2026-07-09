const crud = require("./crudFactory");
module.exports = crud("hojas_ruta_derivaciones", "id_derivacion", ["codigo_barras_seguimiento", "id_documento", "id_empleado_asignado", "id_oficina_procedencia", "fecha_hora_despacho", "prioridad_envio", "fecha_hora_recepcion", "observaciones_receptor", "estado_derivacion", "latitud", "longitud", "updated_at"]);
