create unique index if not exists ux_usuario_empleado_activo
  on usuario (id_empleado)
  where id_empleado is not null and activo = true;
